import { Component, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { AdminService } from '../../../../../../core/services/admin/admin.service';
import { AdminPayment } from '../../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../../components/admin-badge/admin-badge.component';
import { paymentStatusBadge } from '../../../../../../shared/utils/badge/admin-status.utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PaymentStatus } from '../../../../../../core/services/payment/payment.model';

@Component({
  selector: 'app-admin-payment-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-payment-detail.page.html',
  styleUrl: './admin-payment-detail.page.scss',
})
export class AdminPaymentDetailPage implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly payment = signal<AdminPayment | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly refunding = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.load(id);
  }

  private load(id: string): void {

    this.loading.set(true);
    this.error.set(null);

    this.adminService.getPaymentById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({

        next: data => {
          this.payment.set(data);
          this.loading.set(false);
        },

        error: () => {
          this.error.set('Failed to load payment');
          this.loading.set(false);
        }

      });

  }

  statusBadge(status: PaymentStatus) {
    return paymentStatusBadge(status);
  }

  canRefund(): boolean {
    return this.payment()?.paymentStatus === 'CAPTURED';
  }

  refund(): void {

    const p = this.payment();
    if (!p) return;

    if (!confirm('Refund this payment?')) return;

    this.refunding.set(true);

    this.adminService.refundPayment(p.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({

        next: () => {
          this.load(p.id);
        },

        error: () => {
          this.refunding.set(false);
          this.error.set('Refund request failed');
        },

        complete: () => {
          this.refunding.set(false);
        }

      });

  }

}