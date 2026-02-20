import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { TripState } from '../../core/state/trip/trip.state';
import { LoaderService } from '../../core/ui/loader/loader.service';
import { ToastService } from '../../core/ui/toast/toast.service';
import { LeafletMapComponent } from '../../shared/components/map/leaflet-map.component';
import { MapStop } from '../../shared/components/map/leaflet-map.types';

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

  readonly attemptsRemaining = computed(() => {
    const shipment = this.activeShipment();
    if (!shipment) return 5;
    return Math.max(0, 5 - (shipment.deliveryCodeAttempts || 0));
  });

  
  markInTransit(id: string): void {
    this.tripState.markInTransit(id);
    this.toast.success('Shipment marked as in transit');
  }

  openConfirmModal(id: string): void {
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
  }

  confirmDelivery(): void {
    const shipmentId = this.activeShipmentId();
    const code = this.deliveryCode().trim();

    if (!shipmentId) return;

    if (!/^\d{6}$/.test(code)) {
      this.confirmError.set('Please enter a valid 6-digit code');
      return;
    }

    this.confirmLoading.set(true);
    this.confirmError.set(null);

    this.tripState.confirmDelivery(shipmentId, code).subscribe({
      next: () => {
        this.confirmLoading.set(false);
        this.closeConfirmModal();
        this.tripState.refresh();
        this.toast.success('Delivery confirmed successfully');
      },
      error: err => {
        this.confirmLoading.set(false);
        const message = err.error?.message || 'Invalid confirmation code';
        this.confirmError.set(message);
        
        if (message.includes('locked') || message.includes('Maximum')) {
          setTimeout(() => {
            this.closeConfirmModal();
            this.tripState.refresh();
          }, 2000);
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
    
    if (this.confirmError()) {
      this.confirmError.set(null);
    }
  }
}