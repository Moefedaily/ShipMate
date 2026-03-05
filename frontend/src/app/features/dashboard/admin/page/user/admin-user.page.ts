import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdminUser, UserFilterParams, PageResponse } from '../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../core/services/admin/admin.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-users.page.html',
  styleUrl: './admin-users.page.scss',
})
export class AdminUsersPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);

  readonly page     = signal<PageResponse<AdminUser> | null>(null);
  readonly loading  = signal(true);
  readonly error    = signal<string | null>(null);
  readonly actingId = signal<string | null>(null);
  private readonly destroyRef = inject(DestroyRef);

  filters: UserFilterParams = { page: 0, size: 20 };
  searchInput = '';

  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {

        if (params['active'] === 'true')  this.filters.active = true;
        if (params['active'] === 'false') this.filters.active = false;

        if (params['search']) this.searchInput = String(params['search']);

        this.filters = {
          ...this.filters,
          search: this.searchInput || undefined,
          page: 0
        };

        this.load();
      });
  }
  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getUsers(this.filters)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: data => { this.page.set(data); this.loading.set(false); },
      error: ()   => { this.error.set('Failed to load users'); this.loading.set(false); },
    });
  }

  onSearch(): void {
    this.filters = { ...this.filters, search: this.searchInput || undefined, page: 0 };
    this.load();
  }

  onActiveFilter(active: '' | 'true' | 'false'): void {
    const value = active === '' ? undefined : active === 'true';
    this.filters = { ...this.filters, active: value, page: 0 };
    this.load();
  }

  clearFilters(): void {
    this.filters = { page: 0, size: 20 };
    this.searchInput = '';
    this.load();
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

  deactivate(id: string, event: Event): void {
    event.preventDefault();
    if (!confirm('Deactivate this user?')) return;

    this.actingId.set(id);

    this.adminService.deactivateUser(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.actingId.set(null); this.load(); },
        error: () => { this.actingId.set(null); }
      });
  }

  activate(id: string, event: Event): void {
    event.preventDefault();
    if (!confirm('Activate this user?')) return;

    this.actingId.set(id);

    this.adminService.activateUser(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.actingId.set(null); this.load(); },
        error: () => { this.actingId.set(null); }
      });
  }

  userStatusBadge(active: boolean): { label: string; variant: any } {
    return active
      ? { label: 'Active',    variant: 'success' }
      : { label: 'Inactive',  variant: 'danger' };
  }

  userTypeBadge(type: string): { label: string; variant: any } {
    const map: any = {
      SENDER: { label: 'Sender', variant: 'info' },
      DRIVER: { label: 'Driver', variant: 'muted' },
      BOTH:   { label: 'Both',   variant: 'warning' },
    };
    return map[type] ?? { label: type, variant: 'muted' };
  }
}