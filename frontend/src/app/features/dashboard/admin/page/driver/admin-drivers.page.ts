import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';

import { AdminService } from '../../../../../core/services/admin/admin.service';
import { AdminBadgeComponent } from '../../components/admin-badge/admin-badge.component';
import { driverStatusBadge } from '../../../../../shared/utils/badge/admin-status.utils';

import {
  AdminDriverProfile,
  DriverFilterParams,
  DriverStatus,
  PageResponse
} from '../../../../../core/services/admin/admin.models';

@Component({
  selector: 'app-admin-drivers',
  standalone: true,
  imports: [CommonModule, MatIconModule, AdminBadgeComponent],
  templateUrl: './admin-drivers.page.html',
  styleUrl: './admin-drivers.page.scss',
})
export class AdminDriversPage implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);

  readonly page = signal<PageResponse<AdminDriverProfile> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly acting = signal<string | null>(null);

  filterStatus?: DriverStatus;

  ngOnInit(): void {

    this.route.queryParams.subscribe(params => {

      if (params['status']) {
        this.filterStatus = params['status'];
      }

      this.load();

    });

  }

  load(): void {

    this.loading.set(true);

    const params: DriverFilterParams = {
      page: 0,
      size: 20,
      status: this.filterStatus || undefined
    };

    this.adminService.getDrivers(params).subscribe({

      next: data => {
        this.page.set(data);
        this.loading.set(false);
      },

      error: () => {
        this.error.set('Failed to load drivers');
        this.loading.set(false);
      }

    });

  }

  onStatusFilter(status?: DriverStatus): void {

    this.filterStatus = status;
    this.load();

  }

  approve(id: string): void {

    if (!confirm('Approve this driver application?')) return;

    this.acting.set(id);

    this.adminService.approveDriver(id).subscribe({
      next: () => {
        this.acting.set(null);
        this.load();
      },
      error: () => this.acting.set(null)
    });

  }

  reject(id: string): void {

    if (!confirm('Reject this driver application?')) return;

    this.acting.set(id);

    this.adminService.rejectDriver(id).subscribe({
      next: () => {
        this.acting.set(null);
        this.load();
      },
      error: () => this.acting.set(null)
    });

  }

  suspend(id: string): void {

    if (!confirm('Suspend this driver?')) return;

    this.acting.set(id);

    this.adminService.suspendDriver(id).subscribe({
      next: () => {
        this.acting.set(null);
        this.load();
      },
      error: () => this.acting.set(null)
    });

  }

  addStrike(id: string): void {

    const note = prompt('Enter strike reason');

    if (!note) return;

    this.acting.set(id);

    this.adminService.addDriverStrike(id, note).subscribe({
      next: () => {
        this.acting.set(null);
        this.load();
      },
      error: () => this.acting.set(null)
    });

  }

  resetStrikes(id: string): void {

    if (!confirm('Reset driver strikes?')) return;

    this.acting.set(id);

    this.adminService.resetDriverStrikes(id).subscribe({
      next: () => {
        this.acting.set(null);
        this.load();
      },
      error: () => this.acting.set(null)
    });

  }

  statusBadge(status: DriverStatus) {
    return driverStatusBadge(status);
  }

}