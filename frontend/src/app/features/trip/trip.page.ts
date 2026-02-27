import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { TripState } from '../../core/state/trip/trip.state';
import { LoaderService } from '../../core/ui/loader/loader.service';
import { ToastService } from '../../core/ui/toast/toast.service';
import { LeafletMapComponent } from '../../shared/components/map/leaflet-map.component';
import { MapStop } from '../../shared/components/map/leaflet-map.types';
import { ShipmentResponse } from '../../core/services/shipment/shipment.models';

@Component({
  standalone: true,
  selector: 'app-trip-page',
  imports: [CommonModule, MatIconModule, LeafletMapComponent, RouterLink],
  providers: [TripState],
  templateUrl: './trip.page.html',
  styleUrl: './trip.page.scss'
})
export class TripPage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tripState = inject(TripState);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly booking = this.tripState.booking;
  readonly shipments = this.tripState.sortedShipments;
  readonly loading = this.tripState.loading;
  readonly errorMessage = this.tripState.errorMessage;
  readonly status = this.tripState.status;

  readonly confirmModalOpen = signal(false);
  readonly activeShipmentId = signal<string | null>(null);
  readonly deliveryCode = signal('');
  readonly confirmLoading = signal(false);
  readonly confirmError = signal<string | null>(null);

  readonly maxAttempts = 5;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.toast.error('Invalid trip');
      this.router.navigate(['/dashboard/driver']);
      return;
    }
    this.tripState.load(id);
  }

  readonly statusBadgeClass = computed(() => {
    switch (this.status()) {
      case 'PENDING': return 'badge--warning';
      case 'CONFIRMED':
      case 'IN_PROGRESS': return 'badge--info';
      case 'COMPLETED': return 'badge--success';
      case 'CANCELLED': return 'badge--danger';
      default: return 'badge--neutral';
    }
  });

  readonly mapStops = computed<MapStop[]>(() => {
    const b = this.booking();
    if (!b) return [];

    const stops: MapStop[] = [];
    let order = 1;

    for (const shipment of this.shipments()) {
      stops.push({
        id: `${shipment.id}-pickup`,
        type: 'PICKUP',
        order: order++,
        lat: shipment.pickupLatitude,
        lng: shipment.pickupLongitude,
        address: shipment.pickupAddress,
        shipments: [{
          id: shipment.id,
          label: shipment.packageDescription || 'Shipment',
          weightKg: shipment.packageWeight,
          pickupAddress: shipment.pickupAddress,
          deliveryAddress: shipment.deliveryAddress
        }]
      });

      stops.push({
        id: `${shipment.id}-delivery`,
        type: 'DELIVERY',
        order: order++,
        lat: shipment.deliveryLatitude,
        lng: shipment.deliveryLongitude,
        address: shipment.deliveryAddress,
        shipments: [{
          id: shipment.id,
          label: shipment.packageDescription || 'Shipment',
          weightKg: shipment.packageWeight,
          pickupAddress: shipment.pickupAddress,
          deliveryAddress: shipment.deliveryAddress
        }]
      });
    }

    return stops;
  });

  readonly activeShipment = computed(() => {
    const id = this.activeShipmentId();
    if (!id) return null;
    return this.shipments().find(s => s.id === id) ?? null;
  });

  readonly attemptsUsed = computed(() => {
    return this.activeShipment()?.deliveryCodeAttempts ?? 0;
  });

  readonly attemptsRemaining = computed(() => {
    return Math.max(0, this.maxAttempts - this.attemptsUsed());
  });

  readonly isLocked = computed(() => {
    const s = this.activeShipment();
    if (!s) return false;
    const attempts = s.deliveryCodeAttempts ?? 0;
    return s.deliveryLocked || attempts >= this.maxAttempts;
  });

  readonly attemptsProgressPercent = computed(() => {
    return (this.attemptsUsed() / this.maxAttempts) * 100;
  });

  readonly attemptsSeverityClass = computed(() => {
    const used = this.attemptsUsed();
    if (used >= 5) return 'danger';
    if (used >= 3) return 'warning';
    return 'safe';
  });

  canConfirmShipment(shipment: ShipmentResponse): boolean {
    const attempts = shipment.deliveryCodeAttempts ?? 0;

    return (
      shipment.status === 'IN_TRANSIT' &&
      !shipment.deliveryLocked &&
      attempts < this.maxAttempts &&
      !this.isActing(shipment.id)
    );
  }

  markInTransit(id: string): void {
    this.tripState.markInTransit(id);
    this.toast.success('Shipment marked as in transit');
  }

  openConfirmModal(id: string): void {
    const shipment = this.shipments().find(s => s.id === id);
    if (!shipment) return;

    const attempts = shipment.deliveryCodeAttempts ?? 0;

    // ✅ Hard guard even if button was clickable by mistake
    if (shipment.deliveryLocked || attempts >= this.maxAttempts) {
      this.toast.error('Delivery confirmation is locked');
      return;
    }

    this.activeShipmentId.set(id);
    this.deliveryCode.set('');
    this.confirmError.set(null);
    this.confirmModalOpen.set(true);
  }

  closeConfirmModal(): void {
    this.confirmModalOpen.set(false);
    this.activeShipmentId.set(null);
    this.deliveryCode.set('');
    this.confirmError.set(null);
    this.confirmLoading.set(false);
  }

  confirmDelivery(): void {
    const shipmentId = this.activeShipmentId();
    const code = this.deliveryCode().trim();

    if (!shipmentId) return;

    if (!/^\d{6}$/.test(code)) {
      this.confirmError.set('Please enter a valid 6-digit code');
      return;
    }

    // ✅ If already locked, don’t call backend
    if (this.isLocked()) {
      this.confirmError.set('Delivery confirmation is locked');
      return;
    }

    this.confirmLoading.set(true);
    this.confirmError.set(null);

    this.tripState.confirmDelivery(shipmentId, code).subscribe({
      next: () => {
        this.confirmLoading.set(false);
        this.closeConfirmModal();
        this.tripState.refresh(); // update UI with DELIVERED
        this.toast.success('Delivery confirmed successfully');
      },
      error: err => {
        this.confirmLoading.set(false);

        const message = err.error?.message || 'Invalid confirmation code';
        this.confirmError.set(message);

        // ✅ THIS IS THE MAIN FIX: refresh after EVERY wrong attempt
        this.tripState.refresh();

        // If it became locked, close modal shortly after to show banner
        if (message.toLowerCase().includes('locked') || message.toLowerCase().includes('maximum')) {
          setTimeout(() => this.closeConfirmModal(), 1200);
        }
      }
    });
  }

  cancelShipment(id: string): void {
    const ok = confirm('Cancel this shipment?');
    if (!ok) return;

    this.tripState.cancelShipment(id);
    this.toast.success('Shipment cancelled');
  }

  isActing(id: string): boolean {
    return this.tripState.isActing(id);
  }

  getStatusIcon(): string {
    const statusIconMap: Record<string, string> = {
      'PENDING': 'schedule',
      'CONFIRMED': 'event_available',
      'IN_PROGRESS': 'local_shipping',
      'COMPLETED': 'check_circle',
      'CANCELLED': 'cancel'
    };

    return statusIconMap[this.status() || ''] || 'help_outline';
  }

  onCodeInput(value: string): void {
    const cleaned = value.replace(/\D/g, '').slice(0, 6);
    this.deliveryCode.set(cleaned);

    if (this.confirmError()) this.confirmError.set(null);
  }
}