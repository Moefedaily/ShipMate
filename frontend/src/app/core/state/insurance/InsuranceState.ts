import { Injectable, signal, computed, inject } from '@angular/core';

import { InsuranceService } from '../../services/insurance/insurance.service';
import { InsuranceClaim, CreateInsuranceClaimRequest } from '../../services/insurance/insurance.model';

@Injectable({ providedIn: 'root' })
export class InsuranceState {

  private readonly insuranceService = inject(InsuranceService);

  readonly claim = signal<InsuranceClaim | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly claimExists = signal<boolean>(false);

  readonly isSubmitted = computed(
    () => this.claim()?.claimStatus === 'SUBMITTED'
  );

  readonly isUnderReview = computed(
    () => this.claim()?.claimStatus === 'UNDER_REVIEW'
  );

  readonly isApproved = computed(
    () => this.claim()?.claimStatus === 'APPROVED'
  );

  readonly isRejected = computed(
    () => this.claim()?.claimStatus === 'REJECTED'
  );

  readonly isPaid = computed(
    () => this.claim()?.claimStatus === 'PAID'
  );

  readonly isWaitingReview = computed(() =>
    this.claim()?.claimStatus === 'SUBMITTED' ||
    this.claim()?.claimStatus === 'UNDER_REVIEW'
  );

  readonly isRefundPending = computed(() =>
    this.claim()?.claimStatus === 'APPROVED'
  );

  loadClaim(shipmentId: string): void {
    if (this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.insuranceService.getClaimByShipment(shipmentId).subscribe({
      next: claim => {
        this.claim.set(claim);
        this.claimExists.set(true);
        this.loading.set(false);
      },
      error: err => {
        if (err.status === 404) {
          this.claim.set(null);
          this.claimExists.set(false);
        } else {
          this.errorMessage.set('Failed to load insurance claim');
        }
        this.loading.set(false);
      }
    });
  }

  submitClaim(
    shipmentId: string,
    request: CreateInsuranceClaimRequest
  ): void {

    if (this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.insuranceService.submitClaim(shipmentId, request).subscribe({
      next: claim => {
        this.claim.set(claim);
        this.claimExists.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to submit insurance claim');
        this.loading.set(false);
      }
    });
  }

  clear(): void {
    this.claim.set(null);
    this.claimExists.set(false);
    this.errorMessage.set(null);
    this.loading.set(false);
  }
}