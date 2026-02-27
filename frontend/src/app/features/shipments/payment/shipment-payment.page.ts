import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  computed,
  effect,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { PaymentState } from '../../../core/state/payment/payment.state';
import { StripeService } from '../../../core/services/stripe/stripe.service';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';

@Component({
  standalone: true,
  selector: 'app-shipment-payment-page',
  imports: [CommonModule, MatIconModule, RouterLink],
  providers: [PaymentState],
  templateUrl: './shipment-payment.page.html',
  styleUrl: './shipment-payment.page.scss'
})
export class ShipmentPaymentPage implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly paymentState = inject(PaymentState);
  private readonly stripeService = inject(StripeService);
  private readonly toast = inject(ToastService);
  private readonly shipmentService = inject(ShipmentService);

  readonly loading = computed(() => this.paymentState.loading());
  readonly payment = this.paymentState.payment;
  readonly errorMessage = this.paymentState.errorMessage;
  readonly status = this.paymentState.paymentStatus;
  readonly isAuthorized = this.paymentState.isAuthorized;
  readonly isCaptured = this.paymentState.isCaptured;
  readonly isWaitingAuth = this.paymentState.isWaitingAuth;
  readonly shipment = signal<ShipmentResponse | null>(null);
  readonly shipmentLoading = signal(false);
  readonly shipmentError = signal<string | null>(null);
    private destroyed = false;

  readonly statusBadgeClass = computed(() => {
    switch (this.status()) {
      case 'REQUIRED': return 'badge-required';
      case 'PROCESSING': return 'badge-processing animated';
      case 'AUTHORIZED': return 'badge-authorized';
      case 'CAPTURED': return 'badge-captured';
      case 'FAILED': return 'badge-failed';
      case 'REFUNDED': return 'badge-refunded';
      case 'CANCELLED': return 'badge-cancelled';
      default: return 'badge-neutral';
    }
  });

  readonly canInitialize = computed(() =>
    this.status() === 'REQUIRED' || this.status() === 'FAILED'
  );

  readonly canConfirm = computed(() =>
    this.status() === 'PROCESSING' &&
    !this.isWaitingAuth() &&
    !!this.paymentState.clientSecret()
  );


  private shipmentId!: string;

  private elements: any = null;
  private paymentElement: any = null;

  private mountedClientSecret: string | null = null;

  private _stripeMountEffect = effect(() => {
    const clientSecret = this.paymentState.clientSecret();

    if (!clientSecret) return;

    if (this.mountedClientSecret === clientSecret) return;

    (async () => {
      this.unmountStripeElement();

      const elements = await this.stripeService.createElements(clientSecret);
      if (!elements || this.destroyed) return;

      this.elements = elements;
      this.paymentElement = elements.create('payment');
      this.paymentElement.mount('#payment-element');
      this.mountedClientSecret = clientSecret;
    })();
  });


  private _redirectEffect = effect(() => {
    if (this.isAuthorized()) {
      setTimeout(() => {
        this.router.navigate(['/dashboard/shipments', this.shipmentId]);
      }, 800);
    }
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.toast.error('Invalid shipment');
      this.router.navigate(['/dashboard']);
      return;
    }

    this.shipmentId = id;
    this.paymentState.load(id);
    this.loadShipment(id);
  }


  ngOnDestroy(): void {
    this.destroyed = true;
    this.unmountStripeElement();
  }

  startPayment(): void {
    if (!this.canInitialize()) return;

    // Important: reset Stripe element state before new intent
    this.unmountStripeElement();
    this.paymentState.createIntent(this.shipmentId);
  }

  async confirmPayment(): Promise<void> {
    const stripe = await this.stripeService.getStripe();

    if (!stripe || !this.elements) {
      this.toast.error('Payment form not ready. Initialize payment first.');
      return;
    }

    const result = await stripe.confirmPayment({
      elements: this.elements,
      redirect: 'if_required'
    });

    if (result.error) {
      this.toast.error(result.error.message ?? 'Payment failed');
      return;
    }

    this.paymentState.onStripeConfirmSuccess(this.shipmentId);
  }

  private loadShipment(id: string): void {
    this.shipmentLoading.set(true);
    this.shipmentError.set(null);

    this.shipmentService.getMyShipment(id).subscribe({
      next: shipment => {
        this.shipment.set(shipment);
        this.shipmentLoading.set(false);
      },
      error: err => {
        this.shipmentError.set(
          err.error?.message || 'Failed to load shipment'
        );
        this.shipmentLoading.set(false);
      }
    });
  }
  private unmountStripeElement(): void {
    try {
      if (this.paymentElement) {
        this.paymentElement.unmount();
      }
    } catch {
      // ignore
    }

    this.paymentElement = null;
    this.elements = null;
    this.mountedClientSecret = null;
  }
}
