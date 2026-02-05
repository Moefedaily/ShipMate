import { Component, signal, computed, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { LoaderService } from '../../../core/ui/loader/loader.service';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { LeafletMapComponent } from '../../../shared/components/map/leaflet-map.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { AddressAutocompleteComponent, AddressResult } from '../../../shared/components/address-autocomplete/address-autocomplete.component';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';



@Component({
  standalone: true,
  selector: 'app-shipment-create-page',
  imports: [CommonModule, ReactiveFormsModule,LeafletMapComponent,AddressAutocompleteComponent,MatIconModule],
  templateUrl: './shipment-create.page.html',
  styleUrl: './shipment-create.page.scss'
})
export class ShipmentCreatePage {

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shipmentService = inject(ShipmentService);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);
  readonly submitting = signal(false);
  readonly estimatedPrice = signal<number | null>(null);
  readonly pricingLoading = signal(false);
  readonly pricingDistanceKm = signal<number | null>(null);


  /* ---------------- Map state ---------------- */

  readonly pickup = signal<AddressResult | null>(null);
  readonly delivery = signal<AddressResult | null>(null);

  /* ---------------- Photos ---------------- */

  readonly photos = signal<File[]>([]);

  /* ---------------- Form ---------------- */

    readonly form = this.fb.nonNullable.group({
    packageDescription: [''],
    packageWeight: [1, [Validators.required, Validators.min(0.01)]],
    packageValue: [0, [Validators.required, Validators.min(0)]],
    requestedPickupDate: ['', Validators.required],
    requestedDeliveryDate: ['', Validators.required],
    basePrice: [0, [Validators.required, Validators.min(0)]]
    });

    /* ---------------- Form validity ---------------- */

    readonly formValueTick = toSignal(
      this.form.valueChanges.pipe(debounceTime(300)),
      { initialValue: this.form.getRawValue() }
    );

    readonly weight = toSignal(
      this.form.controls.packageWeight.valueChanges.pipe(debounceTime(300)),
      { initialValue: this.form.controls.packageWeight.value }
    );

    /* ---------------- Date validation ---------------- */

    private parseDateOnly(value: string | null | undefined): number | null {
    if (!value) return null;

    const parts = value.split('-');
    if (parts.length !== 3) return null;

    const y = Number(parts[0]);
    const m = Number(parts[1]);
    const d = Number(parts[2]);

    if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return null;

    return Date.UTC(y, m - 1, d);
    }

    readonly dateOrderValid = computed(() => {
    this.formValueTick();

    const pRaw = this.form.controls.requestedPickupDate.value;
    const dRaw = this.form.controls.requestedDeliveryDate.value;

    const p = this.parseDateOnly(pRaw);
    const d = this.parseDateOnly(dRaw);

    if (p === null || d === null) return false;

    return d >= p; 
    });


   readonly canSubmit = computed(() => {
    this.formValueTick();

    return (
        this.form.valid &&
        this.dateOrderValid() &&
        !!this.pickup() &&
        !!this.delivery()
    );
    });


  readonly photoPreviews = computed(() =>
    this.photos().map(file => URL.createObjectURL(file))
    );

    constructor() {
  effect(() => {
    const pickup = this.pickup();
    const delivery = this.delivery();
    const weight = this.weight();

    if (!pickup || !delivery || !weight || weight <= 0) {
      this.estimatedPrice.set(null);
      this.pricingDistanceKm.set(null);
      return;
    }

    this.pricingLoading.set(true);

    this.shipmentService.estimate({
        pickupLatitude: pickup.lat,
        pickupLongitude: pickup.lng,
        deliveryLatitude: delivery.lat,
        deliveryLongitude: delivery.lng,
        packageWeight: weight
      }).subscribe({
        next: res => {
          this.estimatedPrice.set(res.estimatedBasePrice);
          this.pricingDistanceKm.set(res.distanceKm);
          this.pricingLoading.set(false);
        },
        error: () => {
          this.estimatedPrice.set(null);
          this.pricingDistanceKm.set(null);
          this.pricingLoading.set(false);
        }
      });
  });
}



  goBack(): void {
    this.router.navigate(['/dashboard/sender']);
  }

  onPickupSelected(addr: AddressResult): void {
   this.pickup.set({...addr});
  }

  onDeliverySelected(addr: AddressResult): void {
   this.delivery.set({...addr});
  }
  onPhotosAdded(files: File[]): void {
    this.photos.update(list => [...list, ...files]);
  }
  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
        this.onPhotosAdded(Array.from(input.files));
        input.value = '';
    }
  }

  removePhoto(index: number): void {
    this.photos.update(list => list.filter((_, i) => i !== index));
  }
  

  submit(): void {
    if (!this.canSubmit() || this.submitting()) {
        this.form.markAllAsTouched();
        return;
    }

    this.submitting.set(true);
    this.loader.show();

    const pickup = this.pickup()!;
    const delivery = this.delivery()!;
    const payload = this.form.getRawValue();

    this.shipmentService.create({
        pickupAddress: pickup.address,
        pickupLatitude: pickup.lat,
        pickupLongitude: pickup.lng,
        deliveryAddress: delivery.address,
        deliveryLatitude: delivery.lat,
        deliveryLongitude: delivery.lng,
        ...payload
    }).subscribe({
        next: shipment => {
        const finish = () => {
            this.loader.hide();
            this.submitting.set(false);
            this.toast.success('Shipment created successfully');
          setTimeout(() => {
              this.router.navigate(['/dashboard/sender']);
            }, 300);
          };
        if (this.photos().length > 0) {
            this.shipmentService.uploadPhotos(shipment.id, this.photos())
            .subscribe({
                next: finish,
                error: () => {
                this.loader.hide();
                this.submitting.set(false);
                this.toast.error('Shipment created but photos upload failed');
                }
            });
        } else {
            finish();
        }
        },
        error: err => {
        this.loader.hide();
        this.submitting.set(false);
        this.toast.error(err.error?.message || 'Failed to create shipment');
        }
    });
    }

}
