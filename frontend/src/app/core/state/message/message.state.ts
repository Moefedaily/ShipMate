import { Injectable, signal, computed, inject } from '@angular/core';
import { Subscription } from 'rxjs';

import { WsService } from '../../services/ws/ws.service';
import { MessageService } from '../../services/message/message.service';

import { MessageWsDto } from '../../services/ws/ws.models';
import { MessageResponse } from '../../services/message/message.models';

type ChatMessage = MessageWsDto | MessageResponse;

@Injectable()
export class MessageState {

  private readonly ws = inject(WsService);
  private readonly messageService = inject(MessageService);

  private wsSub?: Subscription;
  private currentBookingId?: string;
  private historyLoaded = false;
  private loading = false;

  readonly messages = signal<ChatMessage[]>([]);
  readonly errorMessage = signal<string | null>(null);

  readonly hasMessages = computed(
    () => this.messages().length > 0
  );

  // PUBLIC API

  loadForBooking(bookingId: string): void {
    if (this.currentBookingId === bookingId) return;

    this.clear();
    this.currentBookingId = bookingId;

    this.loadHistory(bookingId);
    this.listenToMessages(bookingId);
    this.markAsRead();
  }

  sendMessage(text: string): void {
    const bookingId = this.currentBookingId;
    if (!bookingId || !text.trim()) return;

    this.messageService.sendMessage(bookingId, text).subscribe({

      // wilk do nothing on success! WS echo will arrive automatically
      error: () => {
        this.errorMessage.set('Failed to send message');
      }
    });
  }

  // REST HISTORY

  private loadHistory(bookingId: string): void {
    if (this.historyLoaded || this.loading) return;

    this.loading = true;

    this.messageService.getBookingMessages(bookingId).subscribe({
      next: page => {
        this.messages.set(page.content);
        this.historyLoaded = true;
        this.loading = false;
      },
      error: () => {
        this.errorMessage.set('Failed to load messages');
        this.loading = false;
      }
    });
  }

  private markAsRead(): void {
    if (!this.currentBookingId) return;

    this.messageService.markAsRead(this.currentBookingId).subscribe();
  }

  // WS LISTENER
  private listenToMessages(bookingId: string): void {
    if (this.wsSub) return;

    this.wsSub =
      this.ws.subscribe<MessageWsDto>(
        `/topic/bookings/${bookingId}/messages`
      ).subscribe({
        next: msg => {
          this.messages.update(list => {
            if (list.some(m => m.id === msg.id)) return list;
            return [...list, msg];
          });
        },
        error: () => {
          this.errorMessage.set('Message stream error');
        }
      });
  }

  // RESET
  private clear(): void {
    this.wsSub?.unsubscribe();
    this.wsSub = undefined;

    this.currentBookingId = undefined;
    this.historyLoaded = false;
    this.loading = false;

    this.messages.set([]);
    this.errorMessage.set(null);
  }
}
