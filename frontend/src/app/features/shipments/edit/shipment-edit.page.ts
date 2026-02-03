import { Component, OnInit, inject, signal, computed } from '@angular/core';
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


  readonly original = signal<{
    pickupAddress: string;
    deliveryAddress: string;
    packageDescription: string;
    packageWeight: number;
    requestedPickupDate: string;
    requestedDeliveryDate: string;
  } | null>(null);


  readonly form = this.fb.nonNullable.group({
    packageDescription: [''],
    packageWeight: [1, [Validators.required, Validators.min(0.01)]],
    requestedPickupDate: ['', Validators.required],
    requestedDeliveryDate: ['', Validators.required]
  });


  readonly formTick = toSignal(
    this.form.valueChanges,
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
          return;
        }

        this.shipment.set(shipment);

        this.original.set({
          pickupAddress: shipment.pickupAddress,
          deliveryAddress: shipment.deliveryAddress,
          packageDescription: shipment.packageDescription ?? '',
          packageWeight: shipment.packageWeight,
          requestedPickupDate: shipment.requestedPickupDate,
          requestedDeliveryDate: shipment.requestedDeliveryDate
        });

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

  // CHANGE DETECTION 

  readonly hasChanges = computed(() => {
    const original = this.original();
    if (!original) return false;

    this.formTick();

    if (
      original.packageDescription !== (this.form.value.packageDescription ?? '') ||
      original.packageWeight !== this.form.value.packageWeight ||
      original.requestedPickupDate !== this.form.value.requestedPickupDate ||
      original.requestedDeliveryDate !== this.form.value.requestedDeliveryDate
    ) {
      return true;
    }

    if (
      this.pickup()?.address !== original.pickupAddress ||
      this.delivery()?.address !== original.deliveryAddress
    ) {
      return true;
    }

    return false;
  });


  readonly canSubmit = computed(() => {
    this.formTick();

    return (
      this.form.valid &&
      !!this.pickup() &&
      !!this.delivery() &&
      this.hasChanges() &&
      !this.submitting()
    );
  });

  submit(): void {
    if (!this.canSubmit()) return;

    const shipment = this.shipment();
    if (!shipment) return;

    this.submitting.set(true);
    this.loader.show();

    this.shipmentService.updateShipment(
      shipment.id,
      {
        pickupAddress: this.pickup()!.address,
        deliveryAddress: this.delivery()!.address,
        packageDescription: this.form.value.packageDescription ?? undefined,
        packageWeight: this.form.value.packageWeight!,
        requestedPickupDate: this.form.value.requestedPickupDate!,
        requestedDeliveryDate: this.form.value.requestedDeliveryDate!
      }
    ).subscribe({
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

  /* ================= NAV ================= */

  goBack(): void {
    const id = this.shipment()?.id;
    if (id) {
      this.router.navigate(['/dashboard/shipments', id]);
    } else {
      this.router.navigate(['/dashboard/sender']);
    }
  }
}
