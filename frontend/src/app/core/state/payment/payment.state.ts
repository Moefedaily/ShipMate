import { Injectable, signal, computed, inject, OnDestroy } from '@angular/core';
import {
  timer,
  switchMap,
  takeWhile,
  tap,
  Subscription,
  timeout,
  catchError,
  of
} from 'rxjs';

import { PaymentService } from '../../services/payment/payment.service';
import { PaymentResponse, PaymentStatus } from '../../services/payment/payment.model';

type UiStep =
  | 'IDLE'
  | 'CREATING_INTENT'
  | 'CONFIRMING'
  | 'WAITING_AUTH'
  | 'DONE';

@Injectable()
export class PaymentState implements OnDestroy {

  private readonly paymentService = inject(PaymentService);

  private pollSub?: Subscription;

  readonly payment = signal<PaymentResponse | null>(null);
  readonly clientSecret = signal<string | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly uiStep = signal<UiStep>('IDLE');

  readonly paymentStatus = computed<PaymentStatus>(() =>
    this.payment()?.paymentStatus ?? 'REQUIRED'
  );

  readonly isAuthorized = computed(() =>
    this.paymentStatus() === 'AUTHORIZED'
  );

  readonly isCaptured = computed(() =>
    this.paymentStatus() === 'CAPTURED'
  );

  readonly isPayable = computed(() =>
    this.paymentStatus() === 'REQUIRED' ||
    this.paymentStatus() === 'FAILED'
  );

  readonly isWaitingAuth = computed(() =>
    this.uiStep() === 'WAITING_AUTH'
  );

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /* ============================================================
     LOAD PAYMENT
  ============================================================ */

  load(shipmentId: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.paymentService.getPayment(shipmentId).subscribe({
      next: payment => {
        this.payment.set(payment);
        this.loading.set(false);
      },
      error: err => {
        this.errorMessage.set(
          err.error?.message || 'Failed to load payment'
        );
        this.loading.set(false);
      }
    });
  }

  /* ============================================================
     CREATE INTENT
  ============================================================ */

  createIntent(shipmentId: string): void {

    if (!this.isPayable()) {
      return;
    }

    this.uiStep.set('CREATING_INTENT');
    this.errorMessage.set(null);
    this.clientSecret.set(null);

    this.paymentService.createPaymentIntent(shipmentId).subscribe({
      next: res => {

        this.clientSecret.set(res.clientSecret);
        this.uiStep.set('CONFIRMING');

        // reload payment to reflect PROCESSING state
        this.load(shipmentId);
      },
      error: err => {
        this.errorMessage.set(
          err.error?.message || 'Unable to create payment'
        );
        this.uiStep.set('IDLE');
      }
    });
  }

  /* ============================================================
     STRIPE CONFIRM SUCCESS
  ============================================================ */

  onStripeConfirmSuccess(shipmentId: string): void {

    this.uiStep.set('WAITING_AUTH');
    this.stopPolling();

    const continueStatuses = new Set<PaymentStatus>([
      'PROCESSING'
    ]);

    this.pollSub = timer(0, 1000).pipe(
      switchMap(() => this.paymentService.getPayment(shipmentId)),
      tap(payment => this.payment.set(payment)),
      takeWhile(p => continueStatuses.has(p.paymentStatus), true),
      timeout({ first: 30000, each: 30000 }),
      catchError(() => {
        this.errorMessage.set(
          'Payment confirmation timed out. Please refresh.'
        );
        this.uiStep.set('IDLE');
        return of(null);
      })
    ).subscribe(payment => {

      if (!payment) return;

      switch (payment.paymentStatus) {

        case 'AUTHORIZED':
        case 'CAPTURED':
          this.uiStep.set('DONE');
          this.stopPolling();
          return;

        case 'FAILED':
          this.errorMessage.set(
            payment.failureReason ?? 'Payment failed'
          );
          this.uiStep.set('IDLE');
          this.stopPolling();
          return;

        case 'CANCELLED':
          this.errorMessage.set('Payment was cancelled');
          this.uiStep.set('IDLE');
          this.stopPolling();
          return;

        default:
          return;
      }
    });
  }

  /* ============================================================
     RESET STATE
  ============================================================ */

  reset(): void {
    this.stopPolling();
    this.clientSecret.set(null);
    this.uiStep.set('IDLE');
    this.errorMessage.set(null);
  }

  /* ============================================================
     INTERNAL
  ============================================================ */

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
  }

  refresh(shipmentId: string): void {
    this.load(shipmentId);
  }
}
