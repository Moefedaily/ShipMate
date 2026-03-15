import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AdminDashboardStats } from '../../../../core/services/admin/admin.models';
import { AdminService } from '../../../../core/services/admin/admin.service';

interface StatCard {
  label:    string;
  key:      keyof AdminDashboardStats;
  icon:     string;
  color:    'primary' | 'success' | 'warning' | 'info' | 'danger';
  route:    string;
  prefix?:  string;
  suffix?:  string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './admin-dashboard.page.html',
  styleUrl: './admin-dashboard.page.scss',
})
export class AdminDashboardPage implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly stats   = signal<AdminDashboardStats | null>(null);
  readonly loading = signal(true);
  readonly error   = signal<string | null>(null);

  readonly statCards: StatCard[] = [
    { label: 'Total Users',           key: 'totalUsers',          icon: 'people',            color: 'info',    route: '/admin/users' },
    { label: 'Total Drivers',         key: 'totalDrivers',        icon: 'local_shipping',    color: 'primary', route: '/admin/drivers' },
    { label: 'Active Shipments',      key: 'activeShipments',     icon: 'inventory_2',       color: 'warning', route: '/admin/shipments' },
    { label: 'Completed Shipments',   key: 'completedShipments',  icon: 'check_circle',      color: 'success', route: '/admin/shipments' },
    { label: 'Pending Claims',        key: 'pendingClaims',       icon: 'gavel',             color: 'danger',  route: '/admin/claims' },
    { label: 'Total Payments',        key: 'totalPayments',       icon: 'account_balance',   color: 'info',    route: '/admin/payments' },
    { label: 'Total Revenue',         key: 'totalRevenue',        icon: 'trending_up',       color: 'success', route: '/admin/payments', prefix: '$' },
    { label: 'Pending Approvals',     key: 'pendingApprovals',    icon: 'hourglass_top',     color: 'warning', route: '/admin/drivers' },
  ];

  ngOnInit(): void {
    this.adminService.getDashboardStats().subscribe({
      next: data => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load dashboard stats');
        this.loading.set(false);
      },
    });
  }

  getStatValue(card: StatCard): string {
    const s = this.stats();
    if (!s) return '—';
    const raw = s[card.key];
    const num = typeof raw === 'number' ? raw : 0;
    const formatted = num >= 1000
      ? (num / 1000).toFixed(1) + 'k'
      : num.toLocaleString();
    return `${card.prefix ?? ''}${formatted}${card.suffix ?? ''}`;
  }
}