import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { LoaderService } from '../../core/ui/loader/loader.service';
import { ToastService } from '../../core/ui/toast/toast.service';
import { LeafletMapComponent } from '../../shared/components/map/leaflet-map.component';
import { MapStop } from '../../shared/components/map/leaflet-map.types';
import { MatIconModule } from '@angular/material/icon';
import { BookingState } from '../../core/state/booking/booking.state';

@Component({
  standalone: true,
  selector: 'app-booking-page',
  imports: [CommonModule, LeafletMapComponent, MatIconModule],
  providers: [BookingState],
  templateUrl: './booking.page.html',
  styleUrl: './booking.page.scss'
})
export class BookingPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bookingState = inject(BookingState);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly loading = computed(() => this.bookingState.loading());
  readonly booking = this.bookingState.booking;
  readonly errorMessage = this.bookingState.errorMessage;
  readonly acting = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.toast.error('Invalid booking');
      this.router.navigate(['/dashboard']);
      return;
    }

    this.bookingState.load(id);
  }

  readonly status = computed(() => this.booking()?.status);
  readonly shipments = computed(() => this.booking()?.shipments ?? []);
  readonly totalPrice = computed(() => this.booking()?.totalPrice ?? 0);
  readonly driverEarnings = computed(() => this.booking()?.driverEarnings ?? 0);
  readonly platformCommission = computed(() => this.booking()?.platformCommission ?? 0);

  readonly statusBadgeClass = computed(() => {
    switch (this.status()) {
      case 'PENDING': return 'badge--warning';
      case 'CONFIRMED':
      case 'IN_PROGRESS': return 'badge--info';
      case 'CANCELLED': return 'badge--danger';
      default: return 'badge--neutral';
    }
  });

  readonly mapStops = computed<MapStop[]>(() => {
    const booking = this.booking();
    if (!booking) return [];

    const stops: MapStop[] = [];
    let order = 1;

    for (const shipment of booking.shipments) {
      // PICKUP
      stops.push({
        id: `${shipment.id}-pickup`,
        type: 'PICKUP',
        order: order++,
        lat: shipment.pickupLatitude,
        lng: shipment.pickupLongitude,
        address: shipment.pickupAddress,
        shipments: [{
          id: shipment.id,
          label: shipment.packageDescription || 'Shipment',
          weightKg: shipment.packageWeight,
          pickupAddress: shipment.pickupAddress,
          deliveryAddress: shipment.deliveryAddress
        }]
      });

      // DELIVERY
      stops.push({
        id: `${shipment.id}-delivery`,
        type: 'DELIVERY',
        order: order++,
        lat: shipment.deliveryLatitude,
        lng: shipment.deliveryLongitude,
        address: shipment.deliveryAddress,
        shipments: [{
          id: shipment.id,
          label: shipment.packageDescription || 'Shipment',
          weightKg: shipment.packageWeight,
          pickupAddress: shipment.pickupAddress,
          deliveryAddress: shipment.deliveryAddress
        }]
      });
    }

    return stops;
  });

  confirm(): void {
    this.runAction('confirm', 'Booking confirmed');
  }

  start(): void {
    this.runAction('start', 'Delivery started');
  }

  cancel(): void {
    const ok = confirm('Cancel this booking?');
    if (!ok) return;

    this.runAction('cancel', 'Booking cancelled');
  }

  private runAction( action: 'confirm' | 'start' | 'cancel', successMessage: string ): void {
    
    const booking = this.booking();
    if (!booking) return;

    this.acting.set(true);
    this.loader.show();

    this.bookingState.runAction(action).subscribe({
      next: (updated) => {
        if (!updated) return;

        this.toast.success(successMessage);

        if (action === 'start') {
          this.router.navigate(['/dashboard/trip', updated.id]);
        }

        this.acting.set(false);
        this.loader.hide();
      },
      error: err => {
        this.toast.error(
          err.error?.message || 'Action failed'
        );
        this.acting.set(false);
        this.loader.hide();
      }
    });
  }

}
