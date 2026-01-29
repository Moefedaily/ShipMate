import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { LeafletMapComponent, MapPoint } from '../../../../shared/components/map/leaflet-map.component';
import { MatchingService } from '../../../../core/services/driver/matching/matching.service';
import { MatchResult } from '../../../../core/services/driver/matching/matching.models';
import { LoaderService } from '../../../../core/ui/loader/loader.service';
import { ToastService } from '../../../../core/ui/toast/toast.service';
import { DriverService } from '../../../../core/services/driver/driver.service';
import { LocationUpdateModalComponent } from '../components/location-update-modal/location-update-modal.component';
import { BookingService } from '../../../../core/services/booking/booking.service';

@Component({
  standalone: true,
  selector: 'app-driver-matching-page',
  imports: [CommonModule, MatIconModule, LeafletMapComponent,LocationUpdateModalComponent],
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
  
  readonly showLocationModal = signal(false);
  private pendingAccept: MatchResult | null = null;


  /* ---------------- Data ---------------- */

  readonly matches = signal<MatchResult[]>([]);
  readonly selected = signal<MatchResult | null>(null);

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
    this.ensureLocationThenLoad();
  }

  /* ---------------- Guards ---------------- */

  private ensureLocationThenLoad(): void {
    this.driverService.getMyDriverProfile().subscribe(profile => {
      if (!profile.lastLatitude || !profile.lastLongitude) {
        this.router.navigate(['/dashboard/driver/setup-location']);
        return;
      }

      this.loadMatches();
    });
  }

  /* ---------------- Data loading ---------------- */

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

    accept(match: MatchResult, event: MouseEvent): void {
       
        event.stopPropagation();

        this.pendingAccept = match;
        this.loader.show();

        this.bookingService.createBooking({
            shipmentIds: [match.shipment.id]
        }).subscribe({
            next: booking => {
            this.loader.hide();
            this.pendingAccept = null;
            this.toast.success('Delivery accepted successfully');

            // later: navigate to booking page
            // this.router.navigate(['/dashboard/driver/booking', booking.id]);
            },
            error: err => {
            this.loader.hide();

            if (err.status === 409 &&
                ['LOCATION_REQUIRED', 'LOCATION_OUTDATED', 'LOCATION_TOO_FAR']
                .includes(err.error?.code)) {
                this.showLocationModal.set(true);
                return;
            }

            this.pendingAccept = null;
            this.toast.error(
                err?.error?.message || 'Unable to accept delivery'
            );
            }
        });
        }



    onLocationUpdated(): void {
        
        this.showLocationModal.set(false);

        if (this.pendingAccept) {
            const retry = this.pendingAccept;
            this.pendingAccept = null;
            this.accept(retry, new MouseEvent('click'));
        }
    }

    onLocationCancelled(): void {
        this.showLocationModal.set(false);
        this.pendingAccept = null;
    }




  trackById = (_: number, m: MatchResult) => m.shipment.id;
}
