import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AdminEarning, PageResponse } from '../../../../../core/services/admin/admin.models';
import { AdminService } from '../../../../../core/services/admin/admin.service';

@Component({
  selector: 'app-admin-earnings',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './admin-earnings.page.html',
  styleUrl: './admin-earnings.page.scss',
})
export class AdminEarningsPage implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly page    = signal<PageResponse<AdminEarning> | null>(null);
  readonly loading = signal(true);

  private currentPage = 0;
  private pageSize    = 20;

  // ── Computed totals for the summary chips ──────────────────────────────────
  readonly paidTotal = computed(() =>
    (this.page()?.content ?? [])
      .filter(e => e.paid)
      .reduce((sum, e) => sum + e.amount, 0)
  );

  readonly pendingTotal = computed(() =>
    (this.page()?.content ?? [])
      .filter(e => !e.paid)
      .reduce((sum, e) => sum + e.amount, 0)
  );

  readonly currency = computed(() =>
    this.page()?.content[0]?.currency ?? 'USD'
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getEarnings(this.currentPage, this.pageSize).subscribe(p => {
      this.page.set(p);
      this.loading.set(false);
    });
  }

  nextPage(): void {
    const p = this.page();
    if (!p || p.number >= p.totalPages - 1) return;
    this.currentPage = p.number + 1;
    this.load();
  }

  prevPage(): void {
    const p = this.page();
    if (!p || p.number === 0) return;
    this.currentPage = p.number - 1;
    this.load();
  }

  markPaid(id: string): void {
    if (!confirm('Mark payout as paid?')) return;
    this.adminService.markEarningPaid(id).subscribe(() => this.load());
  }
}