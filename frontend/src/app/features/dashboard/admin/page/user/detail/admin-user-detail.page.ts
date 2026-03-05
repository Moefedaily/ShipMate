import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdminBadgeComponent } from '../../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../../core/services/admin/admin.service';
import {
  AdminUser,
  PageResponse,
  ShipmentStatus,
  ClaimStatus,
  UserShipmentRow,
  UserClaimRow,
  UserPaymentRow
} from '../../../../../../core/services/admin/admin.models';

import { shipmentStatusBadge, claimStatusBadge, paymentStatusBadge } from '../../../../../../shared/utils/badge/admin-status.utils';
import { PaymentStatus } from '../../../../../../core/services/payment/payment.model';


@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-user-detail.page.html',
  styleUrl: './admin-user-detail.page.scss',
})
export class AdminUserDetailPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly user    = signal<AdminUser | null>(null);
  readonly loading = signal(true);
  readonly error   = signal<string | null>(null);
  readonly acting  = signal(false);

  readonly shipments = signal<PageResponse<UserShipmentRow> | null>(null);
  readonly claims    = signal<PageResponse<UserClaimRow> | null>(null);
  readonly payments  = signal<PageResponse<UserPaymentRow> | null>(null);

  shipPage = 0; shipSize = 5;
  claimPage = 0; claimSize = 5;
  payPage = 0; paySize = 5;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    if (!id) return;
    this.loadAll(id);
  }

  private loadAll(userId: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getUserById(userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: u => {
          this.user.set(u);
          this.loading.set(false);

          this.loadShipments();
          this.loadClaims();
          this.loadPayments();
        },
        error: () => { this.error.set('Failed to load user'); this.loading.set(false); },
      });
  }

  deactivate(): void {
    const u = this.user();
    if (!u) return;
    if (!confirm('Deactivate this user?')) return;

    this.acting.set(true);
    this.adminService.deactivateUser(u.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.user.update(x => x ? { ...x, active: false } : x);
          this.acting.set(false);
        },
        error: () => this.acting.set(false)
      });
  }

  activate(): void {
    const u = this.user();
    if (!u) return;
    if (!confirm('Activate this user?')) return;

    this.acting.set(true);
    this.adminService.activateUser(u.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.user.update(x => x ? { ...x, active: true } : x);
          this.acting.set(false);
        },
        error: () => this.acting.set(false)
      });
  }

  loadShipments(): void {
    const u = this.user();
    if (!u) return;

    this.adminService.getUserShipments(u.id, this.shipPage, this.shipSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => this.shipments.set(p),
        error: () => {}
      });
  }

  loadClaims(): void {
    const u = this.user();
    if (!u) return;

    this.adminService.getUserClaims(u.id, this.claimPage, this.claimSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => this.claims.set(p),
        error: () => {}
      });
  }

  loadPayments(): void {
    const u = this.user();
    if (!u) return;

    this.adminService.getUserPayments(u.id, this.payPage, this.paySize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => this.payments.set(p),
        error: () => {}
      });
  }

  nextShip(): void { if (!this.shipments() || this.shipPage >= this.shipments()!.totalPages - 1) return; this.shipPage++; this.loadShipments(); }
  prevShip(): void { if (this.shipPage === 0) return; this.shipPage--; this.loadShipments(); }

  nextClaim(): void { if (!this.claims() || this.claimPage >= this.claims()!.totalPages - 1) return; this.claimPage++; this.loadClaims(); }
  prevClaim(): void { if (this.claimPage === 0) return; this.claimPage--; this.loadClaims(); }

  nextPay(): void { if (!this.payments() || this.payPage >= this.payments()!.totalPages - 1) return; this.payPage++; this.loadPayments(); }
  prevPay(): void { if (this.payPage === 0) return; this.payPage--; this.loadPayments(); }

  userTypeBadge(type: string) {
    const map: any = {
      SENDER: { label: 'Sender', variant: 'info' },
      DRIVER: { label: 'Driver', variant: 'primary' },
      BOTH:   { label: 'Sender & Driver', variant: 'warning' },
    };
    return map[type] ?? { label: type, variant: 'muted' };
  }

  shipmentBadge(status: ShipmentStatus) { return shipmentStatusBadge(status); }
  claimBadge(status: ClaimStatus) { return claimStatusBadge(status); }
  paymentBadge(status: PaymentStatus) { return paymentStatusBadge(status); }

  shortId(id: string): string {
    return id ? `${id.slice(0, 8)}…` : '—';
  }
}