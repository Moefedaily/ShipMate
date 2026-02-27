import { Component, EventEmitter, Output, signal, inject, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FocusTrap, FocusTrapFactory } from '@angular/cdk/a11y';
import { AddressAutocompleteComponent, AddressResult } from '../../../../../shared/components/address-autocomplete/address-autocomplete.component';
import { DriverLocationService } from '../../../../../core/services/driver/location/driver-location.service';


@Component({
  standalone: true,
  selector: 'app-location-update-modal',
  imports: [CommonModule, AddressAutocompleteComponent],
  templateUrl: './location-update-modal.component.html',
  styleUrl: './location-update-modal.component.scss'
})
export class LocationUpdateModalComponent
  implements AfterViewInit, OnDestroy {

  private readonly locationService = inject(DriverLocationService);
  private readonly focusTrapFactory = inject(FocusTrapFactory);

  @ViewChild('modalCard', { static: true })
  private modalCard!: ElementRef<HTMLElement>;

  private focusTrap!: FocusTrap;

  @Output() updated = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedAddress = signal<AddressResult | null>(null);

  /* ---------- Lifecycle ---------- */

  ngAfterViewInit(): void {
    this.focusTrap = this.focusTrapFactory.create(this.modalCard.nativeElement);
    this.focusTrap.focusInitialElement();
  }

  ngOnDestroy(): void {
    this.focusTrap?.destroy();
    this.resetState();
  }

  /* ---------- Actions ---------- */

  useDeviceLocation(): void {
    if (this.loading()) return;

    this.error.set(null);
    this.loading.set(true);

    navigator.geolocation.getCurrentPosition(
      pos => this.save(pos.coords.latitude, pos.coords.longitude),
      () => {
        this.loading.set(false);
        this.error.set(
          'Location access was denied. You can enter your address instead.'
        );
      }
    );
  }

  onAddressSelected(result: AddressResult): void {
    if (this.loading()) return;
    this.selectedAddress.set(result);
  }

  confirmManual(): void {
    if (this.loading()) return;

    const addr = this.selectedAddress();
    if (!addr) return;

    this.save(addr.lat, addr.lng);
  }

  cancel(): void {
    if (this.loading()) return;
    this.cancelled.emit();
  }

  /* ---------- Persist ---------- */

  private save(lat: number, lng: number): void {
    this.locationService.updateMyLocation(lat, lng).subscribe({
      next: () => {
        this.loading.set(false);
        this.updated.emit();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Unable to update location. Please try again.');
      }
    });
  }

  private resetState(): void {
    this.loading.set(false);
    this.error.set(null);
    this.selectedAddress.set(null);
  }
}
