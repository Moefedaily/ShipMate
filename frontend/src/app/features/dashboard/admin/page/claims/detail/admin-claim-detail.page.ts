import { Component, computed, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AdminService } from '../../../../../../core/services/admin/admin.service';
import { AdminClaim, ClaimStatus } from '../../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../../components/admin-badge/admin-badge.component';
import { claimStatusBadge } from '../../../../../../shared/utils/badge/admin-status.utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-admin-claim-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-claim-detail.page.html',
  styleUrl: './admin-claim-detail.page.scss',
})
export class AdminClaimDetailPage implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);
  private readonly destroyRef   = inject(DestroyRef);

  readonly claim   = signal<AdminClaim | null>(null);
  readonly loading = signal(true);
  readonly error   = signal<string | null>(null);
  readonly acting  = signal(false);

  readonly canAct = computed(() => {
    const s = this.claim()?.claimStatus;
    return s === 'SUBMITTED' || s === 'UNDER_REVIEW';
  });

  ngOnInit(): void {

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.adminService
      .getClaimById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          this.claim.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load claim');
          this.loading.set(false);
        },
      });

  }

  approve(): void {

    if (!confirm('Approve this claim?')) return;

    const c = this.claim();
    if (!c) return;

    this.acting.set(true);

    this.adminService
      .approveClaim(c.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.claim.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false),
      });

  }

  reject(): void {

    const reason = prompt('Rejection reason:');
    if (!reason) return;

    const c = this.claim();
    if (!c) return;

    this.acting.set(true);

    this.adminService
      .rejectClaim(c.id, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.claim.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false),
      });

  }

  statusBadge(status: ClaimStatus) {
    return claimStatusBadge(status);
  }

}