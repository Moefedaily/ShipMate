import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthState } from '../../../../../core/auth/auth.state';

export interface NavItem {
  label:  string;
  icon:   string;
  route:  string;
  badge?: number;
}

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatIconModule, MatTooltipModule],
  templateUrl: './admin-sidebar.component.html',
  styleUrl: './admin-sidebar.component.scss',
})
export class AdminSidebarComponent {
  private readonly authState = inject(AuthState);

  readonly expanded = signal(false);

  readonly user = computed(() => this.authState.user());
  readonly userName = computed(() => {
    const u = this.user();
    return u ? `${u.firstName} ${u.lastName}` : 'Admin';
  });

  readonly navItems: NavItem[] = [
    { label: 'Overview',   icon: 'dashboard',        route: '/admin' },
    { label: 'Users',      icon: 'people',            route: '/admin/users' },
    { label: 'Drivers',    icon: 'local_shipping',    route: '/admin/drivers' },
    { label: 'Shipments',  icon: 'inventory_2',       route: '/admin/shipments' },
    { label: 'Bookings',   icon: 'event',            route: '/admin/bookings' },
    { label: 'Claims',     icon: 'gavel',             route: '/admin/claims' },
    { label: 'Payments',   icon: 'account_balance',   route: '/admin/payments' },
    { label: 'Earnings',   icon: 'payments',         route: '/admin/earnings' },
  ];

  toggleExpanded(): void {
    this.expanded.update(v => !v);
  }
}