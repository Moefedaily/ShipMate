import { Injectable, signal, computed, inject } from '@angular/core';
import { Subscription } from 'rxjs';

import { DeliveryCodeWsDto } from '../../services/ws/ws.models';
import { DeliveryCodeWsService } from '../../services/ws/delivery-code-ws.service';
import { WsService } from '../../services/ws/ws.service';
import { ShipmentService } from '../../services/shipment/shipment.service';

type CodeVisibility = 'HIDDEN' | 'VISIBLE';

@Injectable()
export class DeliveryCodeState {

  private readonly ws = inject(DeliveryCodeWsService);
  private readonly wsService = inject(WsService);
  private readonly shipmentService = inject(ShipmentService);

  private sub?: Subscription;

  readonly visibility = signal<CodeVisibility>('HIDDEN');

  readonly shipmentId = signal<string | null>(null);
  readonly code = signal<string | null>(null);
  readonly copied = signal(false);

  readonly hasCode = computed(() => !!this.code());


  init(): void {
    if (this.sub) return;

    this.wsService.connect();

    this.sub = this.ws.watchMyDeliveryCode().subscribe({
      next: payload => this.onDeliveryCode(payload),
      error: err => {
        console.warn('[DELIVERY_CODE] WS error', err);
      }
    });
  }


  private onDeliveryCode(payload: DeliveryCodeWsDto): void {
    this.shipmentId.set(payload.shipmentId);
    this.code.set(payload.code);
    this.copied.set(false);

    this.visibility.set('VISIBLE');
  }


  checkActiveCode(shipmentId: string): void {
    this.shipmentService.getActiveDeliveryCode(shipmentId)
      .subscribe({
        next: response => {
        console.log('REST delivery code response:', response);
          if (!response) return;

          this.shipmentId.set(response.shipmentId);
          this.code.set(response.code);
          this.copied.set(false);
          this.visibility.set('VISIBLE');
        },
        error: err => {
          console.warn('[DELIVERY_CODE] REST fallback failed', err);
        }
      });
  }

  show(): void {
    if (this.code()) {
      this.visibility.set('VISIBLE');
    }
  }

  hide(): void {
    this.visibility.set('HIDDEN');
  }

  clear(): void {
    this.visibility.set('HIDDEN');
    this.shipmentId.set(null);
    this.code.set(null);
    this.copied.set(false);
  }

  async copyToClipboard(): Promise<boolean> {
    const code = this.code();
    if (!code) return false;

    try {
      await navigator.clipboard.writeText(code);
      this.copied.set(true);

      setTimeout(() => this.copied.set(false), 1200);
      return true;
    } catch {
      return false;
    }
  }
}
