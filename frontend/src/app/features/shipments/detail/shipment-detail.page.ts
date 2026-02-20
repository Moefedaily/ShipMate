import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';

import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';
import { LeafletMapComponent } from '../../../shared/components/map/leaflet-map.component';
import { MapStop } from '../../../shared/components/map/leaflet-map.types';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { LoaderService } from '../../../core/ui/loader/loader.service';
import { PaymentState } from '../../../core/state/payment/payment.state';
import { DeliveryCodeState } from '../../../core/state/delivery-code/delivery-code.state';
import { DeliveryCodeWsService } from '../../../core/services/ws/delivery-code-ws.service';


@Component({
  standalone: true,
  selector: 'app-shipment-detail-page',
  providers: [PaymentState,DeliveryCodeState],
  imports: [
    CommonModule,
    MatIconModule,
    LeafletMapComponent],
  templateUrl: './shipment-detail.page.html',
  styleUrl: './shipment-detail.page.scss'
})
export class ShipmentDetailPage implements OnInit {
  
  /* ==================== Inject ==================== */
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly shipmentService = inject(ShipmentService);
  private readonly toast = inject(ToastService);
  private readonly loader = inject(LoaderService);
  private readonly paymentState = inject(PaymentState);
  readonly deliveryCodeState = inject(DeliveryCodeState);

  /* ==================== State ==================== */
  readonly shipment = signal<ShipmentResponse | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly payment = this.paymentState.payment;
  readonly paymentStatus = this.paymentState.paymentStatus;
  readonly canPay = computed(() =>
    this.shipment()?.status === 'ASSIGNED' &&
    (this.paymentStatus() === 'REQUIRED' ||
    this.paymentStatus() === 'FAILED')
  );

  readonly isAuthorized = computed(() =>
    this.paymentStatus() === 'AUTHORIZED'
  );

  readonly isCaptured = computed(() =>
    this.paymentStatus() === 'CAPTURED'
  );

  readonly isRefunded = computed(() =>
    this.paymentStatus() === 'REFUNDED'
  );

  private hasFetchedCode = false;
  private loadedPaymentForShipmentId: string | null = null;

  constructor() {
    effect(() => {
      const s = this.shipment();
      const status = this.paymentStatus();

      if (!s) return;

      if (s.status === 'ASSIGNED' && this.loadedPaymentForShipmentId !== s.id) {
        this.loadedPaymentForShipmentId = s.id;
        this.paymentState.load(s.id);
      }
      if (!this.hasFetchedCode && status === 'AUTHORIZED') {
        this.deliveryCodeState.checkActiveCode(s.id);
      }
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.toast.error('Invalid shipment');
      this.router.navigate(['/dashboard/sender']);
      return;
    }

    this.hasFetchedCode = false;
    this.loadedPaymentForShipmentId = null;

    this.deliveryCodeState.init();
    this.loadShipment(id);
  }

  private loadShipment(id: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.loader.show();

    this.shipmentService.getMyShipment(id)
      .pipe(
        catchError(err => {
          this.errorMessage.set(err.error?.message || 'Unable to load shipment');
          this.toast.error('Unable to load shipment');
          return of(null);
        })
      )
      .subscribe(shipment => {
        this.shipment.set(shipment);

        if (shipment && (shipment.status === 'ASSIGNED' || shipment.status === 'IN_TRANSIT')) {
          this.paymentState.load(shipment.id);

          this.deliveryCodeState.checkActiveCode(shipment.id);
        }

        this.loading.set(false);
        this.loader.hide();
      });
  }


  readonly canEdit = computed(() =>
    this.shipment()?.status === 'CREATED'
  );


  readonly mapStops = computed<MapStop[]>(() => {
    const s = this.shipment();
    if (!s) return [];

    // Validate coordinates exist
    const hasPickupCoords = s.pickupLatitude != null && s.pickupLongitude != null;
    const hasDeliveryCoords = s.deliveryLatitude != null && s.deliveryLongitude != null;


    if (!hasPickupCoords || !hasDeliveryCoords) {
      return [];
    }

    return [
      {
        id: `${s.id}-pickup`,
        type: 'PICKUP',
        order: 1,
        lat: s.pickupLatitude,
        lng: s.pickupLongitude,
        address: s.pickupAddress,
        shipments: [{
          id: s.id,
          label: s.packageDescription || 'Shipment',
          weightKg: s.packageWeight,
          pickupAddress: s.pickupAddress,
          deliveryAddress: s.deliveryAddress
        }]
      },
      {
        id: `${s.id}-delivery`,
        type: 'DELIVERY',
        order: 2,
        lat: s.deliveryLatitude,
        lng: s.deliveryLongitude,
        address: s.deliveryAddress,
        shipments: [{
          id: s.id,
          label: s.packageDescription || 'Shipment',
          weightKg: s.packageWeight,
          pickupAddress: s.pickupAddress,
          deliveryAddress: s.deliveryAddress
        }]
      }
    ];
  });

  goBack(): void {
    this.router.navigate(['/dashboard/shipments']);
  }

  editShipment(): void {
    const s = this.shipment();
    if (!s) return;
    this.router.navigate(['/dashboard/shipments', s.id, 'edit']);
  }
  goToPayment(): void {
    const s = this.shipment();
    if (!s) return;

    this.router.navigate(['/dashboard/shipments', s.id, 'payment']);
  }

}