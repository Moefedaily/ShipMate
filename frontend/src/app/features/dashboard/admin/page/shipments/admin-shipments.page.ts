import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { AdminService } from '../../../../../core/services/admin/admin.service';
import { AdminShipment, PageResponse, ShipmentStatus, ShipmentFilterParams } from '../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../components/admin-badge/admin-badge.component';
import { shipmentStatusBadge } from '../../../../../shared/utils/badge/admin-status.utils';

@Component({
  selector: 'app-admin-shipments',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-shipments.page.html',
  styleUrl: './admin-shipments.page.scss',
})
export class AdminShipmentsPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);

  readonly page = signal<PageResponse<AdminShipment> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  filters: ShipmentFilterParams = { page: 0, size: 20 };

  readonly statusOptions: { label: string; value: ShipmentStatus | '' }[] = [
    { label: 'All',        value: '' },
    { label: 'Created',    value: 'CREATED' },
    { label: 'Assigned',   value: 'ASSIGNED' },
    { label: 'In Transit', value: 'IN_TRANSIT' },
    { label: 'Delivered',  value: 'DELIVERED' },
    { label: 'Cancelled',  value: 'CANCELLED' },
    { label: 'Lost',       value: 'LOST' },
  ];

  ngOnInit(): void {
    // optional: honor query params (?status=IN_TRANSIT)
    this.route.queryParams.subscribe(params => {
      if (params['status']) this.filters.status = params['status'] as ShipmentStatus;
      this.load();
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getShipments(this.filters).subscribe({
      next: data => { this.page.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load shipments'); this.loading.set(false); },
    });
  }

  onStatusFilter(status: ShipmentStatus | ''): void {
    this.filters = { ...this.filters, status: status ? status : undefined, page: 0 };
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

  statusBadge(status: ShipmentStatus) {
    return shipmentStatusBadge(status);
  }

  shortId(id: string): string {
    return id ? `${id.slice(0, 8)}…` : '—';
  }
}