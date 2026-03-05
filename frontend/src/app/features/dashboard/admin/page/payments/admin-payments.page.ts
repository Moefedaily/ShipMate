import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { AdminPayment, PaymentFilterParams, PageResponse } from '../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../core/services/admin/admin.service';
import { paymentStatusBadge } from '../../../../../shared/utils/badge/admin-status.utils';
import { PaymentStatus } from '../../../../../core/services/payment/payment.model';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-payments.page.html',
  styleUrl: './admin-payments.page.scss',
})
export class AdminPaymentsPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);

  readonly page = signal<PageResponse<AdminPayment> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  filters: PaymentFilterParams = { page: 0, size: 20 };

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['page']) this.filters.page = Number(params['page']);
      if (params['size']) this.filters.size = Number(params['size']);
      this.load();
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getPayments(this.filters).subscribe({
      next: data => { this.page.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load payments'); this.loading.set(false); },
    });
  }

  nextPage(): void {
    const p = this.page();
    if (!p || p.number >= p.totalPages - 1) return;
    this.filters = { ...this.filters, page: p.number + 1 };
    this.load();
  }

  prevPage(): void {
    const p = this.page();
    if (!p || p.number === 0) return;
    this.filters = { ...this.filters, page: p.number - 1 };
    this.load();
  }

  statusBadge(status: PaymentStatus) {
    return paymentStatusBadge(status);
  }

  shortId(id: string): string {
    return id ? `${id.slice(0, 8)}…` : '—';
  }
}