import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AdminClaim, ClaimFilterParams, PageResponse, ClaimStatus } from '../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../core/services/admin/admin.service';
import { claimStatusBadge } from '../../../../../shared/utils/badge/admin-status.utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-admin-claims',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-claims.page.html',
  styleUrl: './admin-claims.page.scss',
})
export class AdminClaimsPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);
  private readonly destroyRef   = inject(DestroyRef);

  readonly page    = signal<PageResponse<AdminClaim> | null>(null);
  readonly loading = signal(true);
  readonly error   = signal<string | null>(null);

  filters: ClaimFilterParams = { page: 0, size: 20 };

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['status']) this.filters.status = params['status'] as ClaimStatus;
      this.load();
    });
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getClaims(this.filters).subscribe({
      next: data => { this.page.set(data); this.loading.set(false); },
      error: ()   => { this.error.set('Failed to load claims'); this.loading.set(false); },
    });
  }

  onStatusFilter(status: string): void {
    this.filters = { ...this.filters, status: status as ClaimStatus | undefined, page: 0 };
    this.load();
  }

  approve(id: string): void {
    if (!confirm('Approve this claim?')) return;

    this.adminService
      .approveClaim(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  reject(id: string): void {
    const reason = prompt('Rejection reason:');
    if (!reason) return;

    this.adminService
      .rejectClaim(id, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
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

  statusBadge(status: ClaimStatus) { return claimStatusBadge(status); }
}