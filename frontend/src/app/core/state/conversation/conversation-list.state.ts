import { Injectable, signal, computed, inject, effect } from '@angular/core';
import { catchError, of, finalize, Subscription } from 'rxjs';

import { ConversationService } from '../../services/conversation/conversation.service';
import { ConversationSummary } from '../../services/conversation/conversation.models';
import { ConversationUpdateWsDto } from '../../services/ws/ws.models';
import { AuthState } from '../../auth/auth.state';
import { MessageWsService } from '../../services/ws/message-ws.service';

@Injectable()
export class ConversationListState {

  private readonly conversationService = inject(ConversationService);
  private readonly wsService = inject(MessageWsService);
  private readonly authState = inject(AuthState);

  private conversationSub?: Subscription;

  readonly conversations = signal<ConversationSummary[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly hasConversations = computed(
    () => this.conversations().length > 0
  );

  readonly totalUnread = computed(
    () => this.conversations().reduce((sum, c) => sum + (c.unreadCount ?? 0), 0)
  );

  readonly hasUnread = computed(
    () => this.totalUnread() > 0
  );

  readonly sortedConversations = computed(() => {
    return [...this.conversations()].sort((a, b) => {
      if (!a.lastMessageAt && !b.lastMessageAt) return 0;
      if (!a.lastMessageAt) return 1;
      if (!b.lastMessageAt) return -1;

      return (
        new Date(b.lastMessageAt).getTime() -
        new Date(a.lastMessageAt).getTime()
      );
    });
  });

  constructor() {
    effect(() => {
      const userId = this.authState.user()?.id;

      this.conversationSub?.unsubscribe();
      this.conversationSub = undefined;

      if (!userId) {
        this.conversations.set([]);
        return;
      }

      this.conversationSub =
        this.wsService.watchConversationUpdates(userId)
          .subscribe(update => {
            this.applyConversationUpdate(update);
          });
    });
  }


  load(): void {
    if (this.loading()) return;

    this.errorMessage.set(null);
    this.loading.set(true);

    this.conversationService.getMyConversations()
      .pipe(
        catchError(() => {
          this.errorMessage.set('Failed to load conversations');
          return of([]);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe(list => {
        this.conversations.set(list);
      });
  }

  refresh(): void {
    this.load();
  }


  private applyConversationUpdate(update: ConversationUpdateWsDto) {

    this.conversations.update(list => {

      const exists = list.some(c => c.bookingId === update.bookingId);

      if (!exists) {
        queueMicrotask(() => this.refresh());
        return list;
      }

      return list.map(c =>
        c.bookingId === update.bookingId
          ? {
              ...c,
              bookingStatus: update.bookingStatus,
              lastMessagePreview: update.lastMessagePreview,
              lastMessageAt: update.lastMessageAt,
              unreadCount: update.unreadCount
            }
          : c
      );
    });
  }


  clear(): void {
    this.conversationSub?.unsubscribe();
    this.conversationSub = undefined;

    this.conversations.set([]);
    this.errorMessage.set(null);
    this.loading.set(false);
  }
}
