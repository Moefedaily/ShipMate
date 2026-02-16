import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterModule } from '@angular/router';

import { LeafletMapComponent, MapPoint } from '../../../../shared/components/map/leaflet-map.component';
import { MatchingService } from '../../../../core/services/driver/matching/matching.service';
import { MatchResult } from '../../../../core/services/driver/matching/matching.models';
import { LoaderService } from '../../../../core/ui/loader/loader.service';
import { ToastService } from '../../../../core/ui/toast/toast.service';
import { DriverService } from '../../../../core/services/driver/driver.service';
import { LocationUpdateModalComponent } from '../components/location-update-modal/location-update-modal.component';
import { BookingService } from '../../../../core/services/booking/booking.service';
import { BookingResponse } from '../../../../core/services/booking/booking.models';

@Component({
  standalone: true,
  selector: 'app-driver-matching-page',
  imports: [CommonModule, MatIconModule, LeafletMapComponent, LocationUpdateModalComponent,RouterModule],
  templateUrl: './driver-matching.page.html',
  styleUrl: './driver-matching.page.scss'
})
export class DriverMatchingPage implements OnInit {

  private readonly matchingService = inject(MatchingService);
  private readonly driverService = inject(DriverService);
  private readonly router = inject(Router);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);
  private readonly bookingService = inject(BookingService);

  /* ---------------- Location Modal ---------------- */

  readonly showLocationModal = signal(false);

  private pendingAccept: MatchResult | null = null;

  private pendingReloadAfterLocation = signal(false);

  /* ---------------- Booking State ---------------- */

  readonly activeBooking = signal<BookingResponse | null>(null);

  readonly bookingStatus = computed(() => this.activeBooking()?.status ?? null);


  readonly isBookingLocked = computed(() =>
    this.bookingStatus() === 'CONFIRMED' || this.bookingStatus() === 'IN_PROGRESS'
  );


  readonly isBuildingTrip = computed(() => this.bookingStatus() === 'PENDING');

  /* ---------------- Data ---------------- */

  readonly matches = signal<MatchResult[]>([]);
  readonly selected = signal<MatchResult | null>(null);
  readonly capacityFull = signal(false);

  /* ---------------- UI: Accept state ---------------- */
  readonly acceptingShipmentId = signal<string | null>(null);

  /* ---------------- Map ---------------- */

  readonly pickupPoint = computed<MapPoint | null>(() => {
    const s = this.selected()?.shipment;
    if (!s) return null;

    return {
      lat: s.pickupLatitude,
      lng: s.pickupLongitude,
      label: s.pickupAddress
    };
  });

  readonly deliveryPoint = computed<MapPoint | null>(() => {
    const s = this.selected()?.shipment;
    if (!s) return null;

    return {
      lat: s.deliveryLatitude,
      lng: s.deliveryLongitude,
      label: s.deliveryAddress
    };
  });

  /* ---------------- Lifecycle ---------------- */

  ngOnInit(): void {
    this.bootstrap();
  }

  private bootstrap(): void {
    this.loadActiveBooking(() => {
      this.ensureLocationThenLoad();
    });
  }

  /* ---------------- Guards ---------------- */

  /**
   * If location missing = open modal.
   */
  private ensureLocationThenLoad(): void {
    this.driverService.getMyDriverProfile().subscribe({
      next: profile => {
        const hasLocation = !!profile.lastLatitude && !!profile.lastLongitude;

        if (!hasLocation) {
          this.pendingReloadAfterLocation.set(true);
          this.showLocationModal.set(true);
          return;
        }

        this.loadMatches();
      },
      error: () => {
        // If profile fails
        this.toast.error('Unable to load your driver profile');
      }
    });
  }

  /* ---------------- Data loading ---------------- */

  private loadActiveBooking(after?: () => void): void {
    this.bookingService.getMyActiveBooking().subscribe({
      next: booking => {
        this.activeBooking.set(booking);
        this.capacityFull.set(false);
        after?.();
      },
      error: () => {
        // If it fails
        this.activeBooking.set(null);
        after?.();
      }
    });
  }

  private loadMatches(): void {
    this.loader.show();

    this.matchingService.getNearbyShipments().subscribe({
      next: res => {
        this.matches.set(res);
        this.selected.set(res.length > 0 ? res[0] : null);
        this.loader.hide();
      },
      error: err => {
        this.loader.hide();

        // Backend location constraints = modal
        if (err?.error?.code &&
          ['LOCATION_REQUIRED', 'LOCATION_OUTDATED', 'LOCATION_TOO_FAR'].includes(err.error.code)
        ) {
          this.pendingReloadAfterLocation.set(true);
          this.showLocationModal.set(true);
          return;
        }

        this.toast.error(
          err?.error?.message || 'Unable to load matching shipments'
        );
      }
    });
  }

  /* ---------------- UI actions ---------------- */

  selectMatch(match: MatchResult): void {
    this.selected.set(match);
  }

  
   //Accepting this shipment: prevent double click
  isAcceptDisabled(m: MatchResult): boolean {
    if (this.isBookingLocked()) return true;
    if (this.capacityFull()) return true;
    if (this.acceptingShipmentId() === m.shipment.id) return true;
    return false;
  }



  acceptDisabledReason(): string | null {
    if (this.capacityFull()) {
      return 'Vehicle capacity reached';
    }

    if (this.bookingStatus() === 'CONFIRMED') {
      return 'Trip confirmed — no changes allowed';
    }

    if (this.bookingStatus() === 'IN_PROGRESS') {
      return 'Finish your current delivery to accept more';
    }

    return null;
  }

  accept(m: MatchResult, event: MouseEvent): void {
    event.stopPropagation();

    if (this.isBookingLocked()) return;

    // Prevent double click on same shipment
    if (this.acceptingShipmentId() === m.shipment.id) return;

    this.pendingAccept = m;
    this.acceptingShipmentId.set(m.shipment.id);

    this.loader.show();

    this.bookingService.createBooking({
      shipmentIds: [m.shipment.id]
    }).subscribe({
      next: booking => {
        this.loader.hide();
        this.acceptingShipmentId.set(null);
        this.pendingAccept = null;

        this.toast.success('Delivery added to your trip');

        // Refresh booking state
        this.activeBooking.set(booking);

        // remove accepted shipment locally
        this.matches.set(this.matches().filter(x => x.shipment.id !== m.shipment.id));
      },
      error: err => {
        this.loader.hide();
        this.acceptingShipmentId.set(null);

        // Location errors = modal then retry
        if (err?.error?.code &&
          ['LOCATION_REQUIRED', 'LOCATION_OUTDATED', 'LOCATION_TOO_FAR'].includes(err.error.code)
        ) {
          this.showLocationModal.set(true);
          return;
        }

        this.pendingAccept = null;

        if (err?.error?.message?.includes('Maximum shipments')) {
          this.capacityFull.set(true);
          this.toast.error('Your vehicle is at full capacity');
          return;
        }

        this.toast.error(
          err?.error?.message || 'Unable to accept delivery'
        );

        //  refresh booking in case state changed (confirmed/in progress elsewhere)
        this.loadActiveBooking();
      }
    });
  }

  /* ---------------- Location modal callbacks ---------------- */

  onLocationUpdated(): void {
    this.showLocationModal.set(false);

    //  reload matching
    if (this.pendingReloadAfterLocation()) {
      this.pendingReloadAfterLocation.set(false);
      this.loadMatches();
    }

    // retry accept
    if (this.pendingAccept) {
      const retry = this.pendingAccept;
      this.pendingAccept = null;

      this.accept(retry, new MouseEvent('click'));
    }
  }

  onLocationCancelled(): void {
    this.showLocationModal.set(false);
    this.pendingAccept = null;
    this.pendingReloadAfterLocation.set(false);
    this.acceptingShipmentId.set(null);
  }

  trackById = (_: number, m: MatchResult) => m.shipment.id;
}
