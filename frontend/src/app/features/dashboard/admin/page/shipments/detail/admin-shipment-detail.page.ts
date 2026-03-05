import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AdminShipment, ShipmentStatus } from '../../../../../../core/services/admin/admin.models';
import { AdminBadgeComponent } from '../../../components/admin-badge/admin-badge.component';
import { AdminService } from '../../../../../../core/services/admin/admin.service';
import { shipmentStatusBadge, paymentStatusBadge } from '../../../../../../shared/utils/badge/admin-status.utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-admin-shipment-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink, AdminBadgeComponent],
  templateUrl: './admin-shipment-detail.page.html',
  styleUrl: './admin-shipment-detail.page.scss',
})
export class AdminShipmentDetailPage implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly shipment = signal<AdminShipment | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly acting = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.adminService.getShipmentById(id).subscribe({
      next: data => { this.shipment.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load shipment'); this.loading.set(false); },
    });
  }

  updateStatus(status: ShipmentStatus): void {
    if (!this.shipment()) return;

  const reason = prompt(`Reason (optional) for setting status to ${status}:`);
  const adminNotes = reason?.trim() || undefined;
    this.acting.set(true);

    this.adminService.updateShipmentStatus(this.shipment()!.id, status, adminNotes)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.shipment.set(updated);
          this.acting.set(false);
        },
        error: () => this.acting.set(false),
      });
  }

  availableTransitions(status: ShipmentStatus): ShipmentStatus[] {

    switch (status) {

      case 'CREATED':
        return ['ASSIGNED', 'CANCELLED', 'LOST'];

      case 'ASSIGNED':
        return ['IN_TRANSIT', 'CANCELLED', 'LOST'];

      case 'IN_TRANSIT':
        return ['CANCELLED', 'LOST'];

      default:
        return [];
    }

  }
  statusBadge(status: ShipmentStatus) { return shipmentStatusBadge(status); }
  paymentBadge(status: any) { return paymentStatusBadge(status); }
}