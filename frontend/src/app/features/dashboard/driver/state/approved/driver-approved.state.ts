import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { BookingService } from '../../../../../core/services/booking/booking.service';
import { MatchingService } from '../../../../../core/services/driver/matching/matching.service';
import { MatchResult } from '../../../../../core/services/driver/matching/matching.models';
import { BookingResponse } from '../../../../../core/services/booking/booking.models';

import { LeafletMapComponent } from '../../../../../shared/components/map/leaflet-map.component';
import { MapStop, MapStopShipment } from '../../../../../shared/components/map/leaflet-map.types';
import { DriverLocationConstraintHandler } from '../../../../../core/ui/constraints/driver-location-constraint.handler';
import { LocationUpdateModalComponent } from '../../components/location-update-modal/location-update-modal.component';

type PendingAction = null | 'RETRY_PREVIEW' | 'OPEN_MATCHING';

@Component({
  standalone: true,
  selector: 'app-driver-approved-state',
  imports: [
    CommonModule,
    MatIconModule,
    LeafletMapComponent,
    LocationUpdateModalComponent
  ],
  templateUrl: './driver-approved.state.html',
  styleUrl: './driver-approved.state.scss'
})
export class DriverApprovedState implements OnInit {
  private readonly bookingService = inject(BookingService);
  private readonly matchingService = inject(MatchingService);
  private readonly router = inject(Router);

  readonly matches = signal<MatchResult[]>([]);
  readonly activeBooking = signal<BookingResponse | null>(null);

  /* ================= BOOKING FLAGS ================= */

  readonly hasBooking = computed(() => !!this.activeBooking());

  readonly bookingStatus = computed(
    () => this.activeBooking()?.status ?? null
  );

  readonly isBookingLocked = computed(
    () =>
      this.bookingStatus() === 'CONFIRMED' ||
      this.bookingStatus() === 'IN_PROGRESS'
  );

  readonly isBuildingTrip = computed(
    () => this.bookingStatus() === 'PENDING'
  );

  /* ================= RAW STOPS (exploded from shipments) ================= */

  private readonly rawStops = computed<MapStop[]>(() => {
    const booking = this.activeBooking();
    if (!booking?.shipments?.length) return [];

    const raw: MapStop[] = [];

    for (const s of booking.shipments) {
      const shipment: MapStopShipment = {
        id: s.id,
        label: s.packageDescription || 'Shipment',
        weightKg: s.packageWeight,
        pickupAddress: s.pickupAddress,
        deliveryAddress: s.deliveryAddress
      };

      if (s.pickupOrder != null) {
        raw.push({
          id: `pickup-${s.id}`,
          type: 'PICKUP',
          order: s.pickupOrder,
          lat: s.pickupLatitude,
          lng: s.pickupLongitude,
          address: s.pickupAddress,
          shipments: [shipment]
        });
      }

      if (s.deliveryOrder != null) {
        raw.push({
          id: `delivery-${s.id}`,
          type: 'DELIVERY',
          order: s.deliveryOrder,
          lat: s.deliveryLatitude,
          lng: s.deliveryLongitude,
          address: s.deliveryAddress,
          shipments: [shipment]
        });
      }
    }

    return raw.sort((a, b) => a.order - b.order);
  });

  /* ================= DRIVER STOPS (grouped = ONE truth) ================= */

  readonly driverStops = computed<MapStop[]>(() => {
    const stops = this.rawStops();
    if (!stops.length) return [];

    const key = (s: MapStop) =>
      `${s.type}:${s.lat.toFixed(5)}:${s.lng.toFixed(5)}`;

    const map = new Map<string, MapStop>();

    for (const s of stops) {
      const k = key(s);

      if (!map.has(k)) {
        map.set(k, {
          ...s,
          shipments: [...s.shipments]
        });
        continue;
      }

      const existing = map.get(k)!;
      existing.order = Math.min(existing.order, s.order);
      existing.shipments.push(...s.shipments);
    }

    return [...map.values()].sort((a, b) => a.order - b.order);
  });

  /* ================= SUMMARY (right cards) ================= */

  readonly totalLoadKg = computed(() => {
    const booking = this.activeBooking();
    if (!booking?.shipments?.length) return 0;
    return booking.shipments.reduce((sum, s) => sum + (s.packageWeight ?? 0), 0);
  });

  /* ================= LOCATION MODAL ================= */

  readonly showLocationModal = signal(false);
  readonly locationModalTitle = signal('Update your location');
  readonly locationModalBody = signal('Please update your location to continue.');

  private pendingAction = signal<PendingAction>(null);

  /* ================= LIFECYCLE ================= */

  ngOnInit(): void {
    this.loadActiveBooking();
    this.loadPreview();
  }

  /* ================= ACTIONS ================= */

  goToBooking(id: string): void {
    const status = this.activeBooking()?.status;

    if (status === 'IN_PROGRESS') {
      this.router.navigate(['/dashboard/trip', id]);
      return;
    }

    this.router.navigate(['/dashboard/bookings', id]);
  }

  openMatching(): void {
    if (this.isBookingLocked()) return;
    this.router.navigate(['/dashboard/driver/matching']);
  }

  /* ================= LOCATION MODAL ================= */

  private openLocationModalFromError(
    err: unknown,
    afterUpdate: PendingAction
  ): void {
    const code = DriverLocationConstraintHandler.getCode(err);
    if (!code) return;

    this.locationModalTitle.set(
      DriverLocationConstraintHandler.getModalTitle(code)
    );
    this.locationModalBody.set(
      DriverLocationConstraintHandler.getModalBody(code)
    );

    this.pendingAction.set(afterUpdate);
    this.showLocationModal.set(true);
  }

  onLocationModalUpdated(): void {
    this.showLocationModal.set(false);

    const next = this.pendingAction();
    this.pendingAction.set(null);

    this.loadActiveBooking();

    if (next === 'RETRY_PREVIEW') this.loadPreview();
    if (next === 'OPEN_MATCHING') this.openMatching();
  }

  onLocationModalCancelled(): void {
    this.showLocationModal.set(false);
    this.pendingAction.set(null);
  }

  /* ================= LOADERS ================= */

  private loadActiveBooking(): void {
    this.bookingService.getMyActiveBooking().subscribe(
      booking => this.activeBooking.set(booking)
    );
  }

  private loadPreview(): void {
    this.matchingService.getNearbyShipments().subscribe({
      next: res => this.matches.set(res.slice(0, 3)),
      error: err => {
        if (DriverLocationConstraintHandler.isLocationError(err)) {
          this.openLocationModalFromError(err, 'RETRY_PREVIEW');
        }
      }
    });
  }
}
