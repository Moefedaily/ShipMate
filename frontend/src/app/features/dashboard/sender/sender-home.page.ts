import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthState } from '../../../core/auth/auth.state';

@Component({
  standalone: true,
  selector: 'app-sender-home',
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './sender-home.page.html',
  styleUrl: './sender-home.page.scss'
})
export class SenderHomePage implements OnInit {

  private readonly shipmentService = inject(ShipmentService);
  private readonly authState = inject(AuthState);

  readonly user = computed(() => this.authState.user());

  readonly userName = computed(() => {
    const u = this.user();
    return u ? u.firstName : '';
  });

  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly loading = signal(true);

  readonly activeShipments = computed(() =>
    this.shipments().filter(s =>
      !['DELIVERED', 'CANCELLED'].includes(s.status)
    )
  );

  ngOnInit(): void {
    this.shipmentService.getMyShipments().subscribe({
      next: res => {
        console.log(res);
        this.shipments.set(res.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
