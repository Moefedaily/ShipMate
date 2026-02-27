import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { LoaderService } from '../../../core/ui/loader/loader.service';

import { LeafletMapComponent } from '../../../shared/components/map/leaflet-map.component';
import {
  AddressAutocompleteComponent,
  AddressResult
} from '../../../shared/components/address-autocomplete/address-autocomplete.component';

import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';
import { MatIconModule } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-shipment-edit-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LeafletMapComponent,
    AddressAutocompleteComponent,
    MatIconModule
  ],
  templateUrl: './shipment-edit.page.html',
  styleUrl: './shipment-edit.page.scss'
})
export class ShipmentEditPage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shipmentService = inject(ShipmentService);
  private readonly toast = inject(ToastService);
  private readonly loader = inject(LoaderService);

  readonly shipment = signal<ShipmentResponse | null>(null);
  readonly submitting = signal(false);

  readonly pickup = signal<AddressResult | null>(null);
  readonly delivery = signal<AddressResult | null>(null);

  readonly pricingLoading = signal(false);

  readonly basePrice = signal<number | null>(null);
  readonly insuranceFee = signal<number | null>(null);
  readonly totalPrice = signal<number | null>(null);
  readonly deductibleRate = signal<number | null>(null);
  readonly insuranceRateApplied = signal<number | null>(null);

  readonly pricingDistanceKm = signal<number | null>(null);

  readonly estimatedPrice = computed(() => this.totalPrice());

  readonly form = this.fb.nonNullable.group({
    packageDescription: [''],
    packageWeight: [1, [Validators.required, Validators.min(0.01)]],
    requestedPickupDate: ['', Validators.required],
    requestedDeliveryDate: ['', Validators.required]
  });

  readonly formTick = toSignal(
    this.form.valueChanges.pipe(debounceTime(250)),
    { initialValue: this.form.getRawValue() }
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.toast.error('Invalid shipment');
      this.router.navigate(['/dashboard/sender']);
      return;
    }
    this.loadShipment(id);
  }

  private loadShipment(id: string): void {
    this.loader.show();

    this.shipmentService.getMyShipment(id).subscribe({
      next: shipment => {
        if (shipment.status !== 'CREATED') {
          this.toast.error('This shipment can no longer be edited');
          this.router.navigate(['/dashboard/shipments', shipment.id]);
          this.loader.hide();
          return;
        }

        this.shipment.set(shipment);

        this.form.patchValue({
          packageDescription: shipment.packageDescription ?? '',
          packageWeight: shipment.packageWeight,
          requestedPickupDate: shipment.requestedPickupDate,
          requestedDeliveryDate: shipment.requestedDeliveryDate
        });

        this.pickup.set({
          address: shipment.pickupAddress,
          lat: shipment.pickupLatitude,
          lng: shipment.pickupLongitude
        });

        this.delivery.set({
          address: shipment.deliveryAddress,
          lat: shipment.deliveryLatitude,
          lng: shipment.deliveryLongitude
        });

        this.basePrice.set(null);
        this.insuranceFee.set(null);
        this.totalPrice.set(null);
        this.deductibleRate.set(null);
        this.insuranceRateApplied.set(null);
        this.pricingDistanceKm.set(null);

        this.loader.hide();
      },
      error: () => {
        this.loader.hide();
        this.toast.error('Unable to load shipment');
        this.router.navigate(['/dashboard/sender']);
      }
    });
  }

  onPickupSelected(addr: AddressResult): void {
    this.pickup.set(addr);
  }

  onDeliverySelected(addr: AddressResult): void {
    this.delivery.set(addr);
  }

  readonly pricingInputsChanged = computed(() => {
    const shipment = this.shipment();
    const pickup = this.pickup();
    const delivery = this.delivery();
    const form = this.formTick();

    if (!shipment || !pickup || !delivery) return false;

    return (
      pickup.lat !== shipment.pickupLatitude ||
      pickup.lng !== shipment.pickupLongitude ||
      delivery.lat !== shipment.deliveryLatitude ||
      delivery.lng !== shipment.deliveryLongitude ||
      form.packageWeight !== shipment.packageWeight
    );
  });

  constructor() {
    effect(() => {
      const shipment = this.shipment();
      const pickup = this.pickup();
      const delivery = this.delivery();
      const form = this.formTick();

      if (!shipment || !pickup || !delivery || !this.form.controls.packageWeight.valid) {
        this.resetPricingPreview();
        return;
      }

      if (!this.pricingInputsChanged()) {
        this.resetPricingPreview();
        return;
      }

      const weight = form.packageWeight ?? 0;
      if (weight <= 0) {
        this.resetPricingPreview();
        return;
      }

      this.pricingLoading.set(true);

      this.shipmentService.previewShipmentPricing({
        pickupLatitude: pickup.lat,
        pickupLongitude: pickup.lng,
        deliveryLatitude: delivery.lat,
        deliveryLongitude: delivery.lng,
        packageWeight: weight,

        // insurance preview requires packageValue too
        packageValue: shipment.packageValue,

        // use CURRENT insurance config from shipment (edit doesn’t change it)
        insuranceSelected: shipment.insuranceSelected === true,
        declaredValue: shipment.insuranceSelected ? (shipment.declaredValue ?? undefined) : undefined
      }).subscribe({
        next: res => {
          this.basePrice.set(res.basePrice);
          this.insuranceFee.set(res.insuranceFee);
          this.totalPrice.set(res.totalPrice);
          this.deductibleRate.set(res.deductibleRate);
          this.insuranceRateApplied.set(res.insuranceRateApplied);

          // if your preview endpoint doesn’t return distance, keep old behavior (null)
          // (optional improvement: extend response to include distanceKm)
          this.pricingDistanceKm.set(this.pricingDistanceKm());

          this.pricingLoading.set(false);
        },
        error: () => {
          this.resetPricingPreview();
          this.pricingLoading.set(false);
        }
      });
    });
  }

  private resetPricingPreview(): void {
    this.basePrice.set(null);
    this.insuranceFee.set(null);
    this.totalPrice.set(null);
    this.deductibleRate.set(null);
    this.insuranceRateApplied.set(null);
    this.pricingDistanceKm.set(null);
  }

  readonly canSubmit = computed(() => {
    this.formTick();
    return (
      this.form.valid &&
      !!this.pickup() &&
      !!this.delivery() &&
      this.pricingInputsChanged() &&
      !this.submitting()
    );
  });

  submit(): void {
    if (!this.canSubmit()) return;

    const shipment = this.shipment();
    if (!shipment) return;

    this.submitting.set(true);
    this.loader.show();

    this.shipmentService.updateShipment(shipment.id, {
      pickupAddress: this.pickup()!.address,
      deliveryAddress: this.delivery()!.address,
      packageDescription: this.form.value.packageDescription ?? undefined,
      packageWeight: this.form.value.packageWeight!,
      requestedPickupDate: this.form.value.requestedPickupDate!,
      requestedDeliveryDate: this.form.value.requestedDeliveryDate!
    }).subscribe({
      next: updated => {
        this.toast.success('Shipment updated');
        this.router.navigate(['/dashboard/shipments', updated.id]);
        this.loader.hide();
      },
      error: err => {
        this.submitting.set(false);
        this.loader.hide();
        this.toast.error(err.error?.message || 'Update failed');
      }
    });
  }

  goBack(): void {
    const id = this.shipment()?.id;
    this.router.navigate(id ? ['/dashboard/shipments', id] : ['/dashboard/sender']);
  }
}