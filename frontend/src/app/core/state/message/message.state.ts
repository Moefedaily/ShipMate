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

  private currentBookingId?: string;
  private historyLoaded = false;
  private loading = false;
  private lastTypingSentAt = 0;

  readonly messages = signal<ChatMessage[]>([]);
  readonly typingUser = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly hasMessages = computed(
    () => this.messages().length > 0
  );

  loadForBooking(bookingId: string): void {
    if (this.currentBookingId === bookingId) return;

    this.clear();

    this.currentBookingId = bookingId;

    this.loadHistory(bookingId);
    this.listenToMessages(bookingId);
  }

  sendMessage(text: string): void {
    const bookingId = this.currentBookingId;
    if (!bookingId || !text.trim()) return;

    this.messageService.sendMessage(bookingId, text).subscribe({
      error: () => {
        this.errorMessage.set('Failed to send message');
      }
    });
  }

  sendTyping(): void {
    const bookingId = this.currentBookingId;
    if (!bookingId) return;

    const now = Date.now();

    if (now - this.lastTypingSentAt < 800) return;

    this.lastTypingSentAt = now;

    this.wsService.sendTyping(bookingId);
  }



  private loadHistory(bookingId: string): void {
    if (this.historyLoaded || this.loading) return;

    this.loading = true;

    this.messageService.getBookingMessages(bookingId).subscribe({
      next: page => {
        this.messages.set(page.content);
        this.historyLoaded = true;
        this.loading = false;

        this.markAsRead();
      },
      error: () => {
        this.errorMessage.set('Failed to load messages');
        this.loading = false;
      }
    });
  }

  private markAsRead(): void {
    if (!this.currentBookingId) return;

    this.messageService
      .markAsRead(this.currentBookingId)
      .subscribe();
  }

  private listenToMessages(bookingId: string): void {

    this.wsSub =
      this.wsService.watchBookingMessages(bookingId)
        .subscribe({
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

    this.typingSub =
      this.wsService.watchTyping(bookingId)
        .subscribe(payload => {

          const currentUserId = this.authState.user()?.id;

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

    this.currentBookingId = undefined;
    this.historyLoaded = false;
    this.loading = false;

    this.messages.set([]);
    this.errorMessage.set(null);
  }
}
