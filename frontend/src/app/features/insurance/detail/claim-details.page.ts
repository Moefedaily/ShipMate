import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { LoaderService } from '../../../core/ui/loader/loader.service';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { InsuranceState } from '../../../core/state/insurance/InsuranceState';
import { ClaimStatus } from '../../../core/services/insurance/insurance.model';

@Component({
  standalone: true,
  selector: 'app-claim-details-page',
  imports: [
    CommonModule,
    MatIconModule
  ],
  templateUrl: './claim-details.page.html',
  styleUrl: './claim-details.page.scss'
})
export class ClaimDetailsPage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly insuranceState = inject(InsuranceState);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly claim = this.insuranceState.claim;
  readonly loading = this.insuranceState.loading;

  private readonly statusOrder: ClaimStatus[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'PAID'
  ];

  ngOnInit(): void {
    const shipmentId = this.route.snapshot.paramMap.get('id');
    if (!shipmentId) {
      this.router.navigate(['/dashboard/shipments']);
      return;
    }

    this.loader.show();
    this.insuranceState.loadClaim(shipmentId);

    setTimeout(() => {
      this.loader.hide();
    }, 400);
  }

  goBack(): void {
    const shipmentId = this.route.snapshot.paramMap.get('id');
    this.router.navigate(['/dashboard/shipments', shipmentId]);
  }

  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'SUBMITTED': return 'Waiting for admin review';
      case 'UNDER_REVIEW': return 'Under review';
      case 'APPROVED': return 'Approved — refund requested';
      case 'REJECTED': return 'Rejected';
      case 'PAID': return 'Paid — refund confirmed';
      default: return status || '';
    }
  }

  isStepDone(step: ClaimStatus): boolean {
    const current = this.claim()?.claimStatus;
    if (!current) return false;
    const currentIndex = this.statusOrder.indexOf(current);
    const stepIndex = this.statusOrder.indexOf(step);
    return currentIndex > stepIndex;
  }
}