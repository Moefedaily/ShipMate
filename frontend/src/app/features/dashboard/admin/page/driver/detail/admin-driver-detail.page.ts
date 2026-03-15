import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdminBadgeComponent } from '../../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../../core/services/admin/admin.service';
import {
  AdminDriverProfile,
  AdminVehicle,
  DriverStatus,
} from '../../../../../../core/services/admin/admin.models';
import { driverStatusBadge } from '../../../../../../shared/utils/badge/admin-status.utils';

@Component({
  selector: 'app-admin-driver-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, AdminBadgeComponent],
  templateUrl: './admin-driver-detail.page.html',
  styleUrl: './admin-driver-detail.page.scss',
})
export class AdminDriverDetailPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly driver = signal<AdminDriverProfile | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly acting = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Driver not found');
      this.loading.set(false);
      return;
    }

    this.adminService.getDriverById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: driver => {
          this.driver.set(driver);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load driver request');
          this.loading.set(false);
        }
      });
  }

  statusBadge(status: DriverStatus) {
    return driverStatusBadge(status);
  }

  approve(): void {
    const driver = this.driver();
    if (!driver || !confirm('Approve this driver application?')) return;
    this.acting.set(true);
    this.adminService.approveDriver(driver.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.driver.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false)
      });
  }

  reject(): void {
    const driver = this.driver();
    if (!driver || !confirm('Reject this driver application?')) return;
    this.acting.set(true);
    this.adminService.rejectDriver(driver.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.driver.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false)
      });
  }

  suspend(): void {
    const driver = this.driver();
    if (!driver || !confirm('Suspend this driver?')) return;
    this.acting.set(true);
    this.adminService.suspendDriver(driver.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.driver.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false)
      });
  }

  primaryVehicle(): AdminVehicle | null {
    const driver = this.driver();
    return driver?.activeVehicle ?? driver?.vehicles?.[0] ?? null;
  }
}
