import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';

type FilterStatus = 'all' | 'CREATED' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';

@Component({
  standalone: true,
  selector: 'app-shipment-history-page',
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './shipment-history.page.html',
  styleUrl: './shipment-history.page.scss'
})
export class ShipmentHistoryPage implements OnInit {
  
  /* ==================== Inject ==================== */
  private readonly shipmentService = inject(ShipmentService);

  /* ==================== State ==================== */
  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly loading = signal(true);
  readonly activeFilter = signal<FilterStatus>('all');

  /* ==================== Lifecycle ==================== */
  ngOnInit(): void {
    this.loadShipments();
  }

  /* ==================== Data Loading ==================== */
  private loadShipments(): void {
    this.loading.set(true);
    this.shipmentService.getMyShipments().subscribe({
      next: res => {
        this.shipments.set(res.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  /* ==================== Computed Properties ==================== */
  
  readonly hasShipments = computed(() =>
    this.shipments().length > 0
  );

  readonly filteredShipments = computed(() => {
    const filter = this.activeFilter();
    const all = this.shipments();

    if (filter === 'all') {
      return all;
    }

    return all.filter(s => s.status === filter);
  });

  // Statistics
  readonly inTransitCount = computed(() =>
    this.shipments().filter(s => s.status === 'IN_TRANSIT').length
  );

  readonly deliveredCount = computed(() =>
    this.shipments().filter(s => s.status === 'DELIVERED').length
  );

  readonly pendingCount = computed(() =>
    this.shipments().filter(s => 
      s.status === 'CREATED' || s.status === 'ASSIGNED'
    ).length
  );

  /* ==================== Filter Actions ==================== */
  
  setFilter(status: FilterStatus): void {
    this.activeFilter.set(status);
  }

  countByStatus(status: string): number {
    return this.shipments().filter(s => s.status === status).length;
  }
}