import { Injectable, signal, computed, inject } from '@angular/core';
import { Subscription } from 'rxjs';

import { MessageWsService } from '../../services/ws/message-ws.service';
import { MessageService } from '../../services/message/message.service';
import { MessageResponse } from '../../services/message/message.models';
import { AuthState } from '../../auth/auth.state';

type ChatMessage = MessageResponse;

@Injectable()
export class MessageState {

  private readonly wsService = inject(MessageWsService);
  private readonly messageService = inject(MessageService);
  private readonly authState = inject(AuthState);

  private wsSub?: Subscription;
  private typingSub?: Subscription;
  private typingTimer?: any;

  private currentShipmentId?: string;
  private historyLoadedFor?: string;
  private loading = false;

  private lastTypingSentAt = 0;
  private markReadDoneFor?: string;

  readonly messages = signal<ChatMessage[]>([]);
  readonly typingUser = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly hasMessages = computed(
    () => this.messages().length > 0
  );

  loadForShipment(shipmentId: string): void {
    if (this.currentShipmentId === shipmentId) return;

    this.clear();

    this.currentShipmentId = shipmentId;

    this.loadHistory(shipmentId);
    this.listenToMessages(shipmentId);
  }

  sendMessage(text: string): void {
    const shipmentId = this.currentShipmentId;
    if (!shipmentId || !text.trim()) return;

    this.messageService.sendMessage(shipmentId, text).subscribe({
      error: () => {
        this.errorMessage.set('Failed to send message');
      }
    });
  }

  sendTyping(): void {
    const shipmentId = this.currentShipmentId;
    if (!shipmentId) return;

    const now = Date.now();

    // Prevent spam (send max once every 800ms)
    if (now - this.lastTypingSentAt < 800) return;

    this.lastTypingSentAt = now;

    this.wsService.sendTyping(shipmentId);
  }

  private loadHistory(shipmentId: string): void {
    if (this.loading) return;
    if (this.historyLoadedFor === shipmentId) return;

    this.loading = true;

    this.messageService.getShipmentMessages(shipmentId).subscribe({
      next: page => {
        this.messages.set(page.content);

        this.historyLoadedFor = shipmentId;
        this.loading = false;

        // Mark as read ONCE after history is loaded (prevents double calls)
        this.markAsReadOnce(shipmentId);
      },
      error: () => {
        this.errorMessage.set('Failed to load messages');
        this.loading = false;
      }
    });
  }

  private markAsReadOnce(shipmentId: string): void {
    if (this.markReadDoneFor === shipmentId) return;

    this.markReadDoneFor = shipmentId;

    this.messageService
      .markAsRead(shipmentId)
      .subscribe({
        error: () => {
          // don’t block UI; just allow retry later if needed
          this.markReadDoneFor = undefined;
        }
      });
  }

  private listenToMessages(shipmentId: string): void {

    this.wsSub =
      this.wsService.watchShipmentMessages(shipmentId)
        .subscribe({
          next: msg => {
            this.messages.update(list => {
              if (list.some(m => m.id === msg.id)) return list;
              return [...list, msg];
            });
            if (this.currentShipmentId === shipmentId) {
              this.markAsReadOnce(shipmentId);
            }
          },
          error: () => {
            this.errorMessage.set('Message stream error');
          }
        });

    this.typingSub =
      this.wsService.watchTyping(shipmentId)
        .subscribe(payload => {

          const currentUserId = this.authState.user()?.id;

          // Ignore own typing events
          if (payload.userId === currentUserId) return;

          this.typingUser.set(payload.displayName);

          clearTimeout(this.typingTimer);

          this.typingTimer = setTimeout(() => {
            this.typingUser.set(null);
          }, 1500);
        });
  }

  private clear(): void {

    this.wsSub?.unsubscribe();
    this.wsSub = undefined;

    this.typingSub?.unsubscribe();
    this.typingSub = undefined;

    clearTimeout(this.typingTimer);
    this.typingUser.set(null);

    this.currentShipmentId = undefined;
    this.historyLoadedFor = undefined;
    this.loading = false;

    this.lastTypingSentAt = 0;
    this.markReadDoneFor = undefined;

    this.messages.set([]);
    this.errorMessage.set(null);
  }
}
