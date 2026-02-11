import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AddressAutocompleteComponent, AddressResult } from '../../../../shared/components/address-autocomplete/address-autocomplete.component';
import { DriverLocationService } from '../../../../core/services/driver/location/driver-location.service';
import { MatIconModule } from '@angular/material/icon';


@Component({
  standalone: true,
  selector: 'app-driver-location-setup',
  imports: [
    CommonModule,
    AddressAutocompleteComponent,
    MatIconModule
  ],
  templateUrl: './driver-location-setup.page.html',
  styleUrl: './driver-location-setup.page.scss'
})
export class DriverLocationSetupPage {

  @Output() completed = new EventEmitter<void>();

  private readonly locationService = inject(DriverLocationService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedAddress = signal<AddressResult | null>(null);

  /* ---------- Device location ---------- */

  useDeviceLocation(): void {
    this.error.set(null);
    this.loading.set(true);

    navigator.geolocation.getCurrentPosition(
      pos => {
        this.saveLocation(pos.coords.latitude, pos.coords.longitude);
      },
      () => {
        this.loading.set(false);
        this.error.set(
          'Location access was denied. You can enter your city or address below.'
        );
      }
    );
  }

  /* ---------- Manual address ---------- */

  onAddressSelected(result: AddressResult): void {
    this.selectedAddress.set(result);
  }

  confirmManualLocation(): void {
    const address = this.selectedAddress();
    if (!address) return;

    this.saveLocation(address.lat, address.lng);
  }

  /* ---------- Persist & exit ---------- */

  private saveLocation(lat: number, lng: number): void {
    this.loading.set(true);

    this.locationService.updateMyLocation(lat, lng).subscribe({
      next: () => {
        this.loading.set(false);
        this.completed.emit();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Failed to save location. Please try again.');
      }
    });
  }

}
