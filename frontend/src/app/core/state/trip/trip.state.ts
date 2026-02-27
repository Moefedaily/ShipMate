import { Injectable, signal, computed, inject, effect } from '@angular/core';
import { catchError, finalize, of, Subscription, tap } from 'rxjs';

import { BookingService } from '../../services/booking/booking.service';
import { BookingWsService } from '../../services/ws/booking-ws.service';
import { ShipmentService } from '../../services/shipment/shipment.service';

import { BookingResponse } from '../../services/booking/booking.models';
import { ShipmentResponse } from '../../services/shipment/shipment.models';

type TripAction = 'inTransit' | 'deliver' | 'cancel';

@Injectable()
export class TripState {

  private readonly bookingService = inject(BookingService);
  private readonly bookingWs = inject(BookingWsService);
  private readonly shipmentService = inject(ShipmentService);

  private bookingUpdatesSub?: Subscription;
  private currentBookingId?: string;

  private shipmentSubs: Subscription[] = [];

  readonly booking = signal<BookingResponse | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  private readonly actingMap = signal<Record<string, boolean>>({});

  readonly status = computed(() => this.booking()?.status);

  readonly shipments = computed<ShipmentResponse[]>(() => {
    const b = this.booking();
    return b?.shipments ?? [];
  });

  readonly sortedShipments = computed(() => {
    const list = [...this.shipments()];
    list.sort((a, b) => {
      const ao = a.deliveryOrder ?? 0;
      const bo = b.deliveryOrder ?? 0;
      return ao - bo;
    });
    return list;
  });

  readonly currentActionableShipment = computed(() => {
    return this.sortedShipments().find(s =>
      s.status !== 'DELIVERED' && s.status !== 'CANCELLED'
    ) ?? null;
  });

  readonly isTripActive = computed(() => this.status() === 'IN_PROGRESS');
  readonly isTripCompleted = computed(() => this.status() === 'COMPLETED');
  readonly isTripCancelled = computed(() => this.status() === 'CANCELLED');

  constructor() {
    effect(() => {
      const bookingId = this.booking()?.id;

      if (!bookingId) {
        this.clearUpdates();
        return;
      }

      this.listenToBookingUpdates(bookingId);
      this.listenToShipmentUpdates()
    });
  }

  load(bookingId: string): void {
    this.errorMessage.set(null);
    this.loading.set(true);

    this.bookingService.getById(bookingId).pipe(
      catchError(err => {
        this.errorMessage.set(err.error?.message || 'Unable to load trip');
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe(b => {
      console.log('Booking response from backend:', b);
      this.booking.set(b);
    });
  }

  isActing(shipmentId: string): boolean {
    return !!this.actingMap()[shipmentId];
  }

  markInTransit(shipmentId: string): void {
    this.runShipmentAction(shipmentId, 'inTransit');
  }

  confirmDelivery(shipmentId: string, code: string) {
    return this.shipmentService.confirmDelivery(shipmentId, code);
  }

  cancelShipment(shipmentId: string): void {
    this.runShipmentAction(shipmentId, 'cancel');
  }

  refresh(): void {
    const b = this.booking();
    if (!b) return;
    this.load(b.id);
  }

  clear(): void {
    this.clearUpdates();
    this.booking.set(null);
    this.loading.set(false);
    this.errorMessage.set(null);
    this.actingMap.set({});
  }

  private runShipmentAction(shipmentId: string, action: TripAction): void {
    const booking = this.booking();
    if (!booking) return;

    this.errorMessage.set(null);
    this.setActing(shipmentId, true);

    const call$ = (() => {
      switch (action) {
        case 'inTransit':
          return this.shipmentService.markInTransit(shipmentId);
        case 'deliver':
          return this.shipmentService.markDelivered(shipmentId);
        case 'cancel':
          return this.shipmentService.cancelShipment(shipmentId);
      }
    })();

    call$.pipe(
      tap((updatedShipment: ShipmentResponse) => {
        this.booking.update(b => {
          if (!b) return b;

          const nextShipments = (b.shipments ?? []).map(s =>
            s.id === updatedShipment.id
              ? { ...s, ...updatedShipment }
              : s
          );
          return { ...b, shipments: nextShipments };
        });
      }),
      tap(() => this.refresh()),
      catchError(err => {
        this.errorMessage.set(err.error?.message || 'Action failed');
        return of(null);
      }),
      finalize(() => this.setActing(shipmentId, false))
    ).subscribe();
  }

  private setActing(shipmentId: string, value: boolean): void {
    this.actingMap.update(map => ({
      ...map,
      [shipmentId]: value
    }));
  }

  private clearUpdates(): void {
    this.bookingUpdatesSub?.unsubscribe();
    this.shipmentSubs?.forEach(s => s.unsubscribe());
    this.bookingUpdatesSub = undefined;
    this.shipmentSubs = [];
    this.currentBookingId = undefined;
  }

  private listenToBookingUpdates(bookingId: string): void {
    if (this.currentBookingId === bookingId) return;

    this.currentBookingId = bookingId;

    this.bookingUpdatesSub?.unsubscribe();
    this.bookingUpdatesSub =
      this.bookingWs.watchBooking(bookingId).subscribe(update => {
        console.log('Shipment WS update:', update);
        this.booking.update(b => {
          if (!b) return b;
          if (b.status === update.status) return b;
          return { ...b, status: update.status };
        });
      });
  }


  private listenToShipmentUpdates() {
    this.shipmentSubs.forEach(s => s.unsubscribe());
    this.shipmentSubs = [];

    for (const shipment of this.shipments()) {
      const sub = this.bookingWs
        .watchShipment(shipment.id)
        .subscribe(update => {
          this.booking.update(b => {
            if (!b) return b;

            return {
              ...b,
             shipments: b.shipments.map(s =>
              s.id === update.shipmentId
                ? {
                    ...s,
                    ...(update.status !== undefined && { status: update.status }),
                    ...(update.deliveryLocked !== undefined && { deliveryLocked: update.deliveryLocked }),
                    ...(update.deliveryCodeAttempts !== undefined && {
                      deliveryCodeAttempts: update.deliveryCodeAttempts
                    })
                  }
                : s
            )
            };
          });
        });

      this.shipmentSubs.push(sub);
    }
  }

}
