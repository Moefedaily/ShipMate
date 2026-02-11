import { Injectable, signal, computed, inject } from '@angular/core';
import { catchError, of, finalize } from 'rxjs';

import { ConversationService } from '../../services/conversation/conversation.service';
import { ConversationSummary } from '../../services/conversation/conversation.models';
import { MessageWsDto } from '../../services/ws/ws.models';
import { MessageWsService } from '../../services/ws/message-ws.service';
import { AuthState } from '../../auth/auth.state';

@Injectable()
export class ConversationListState {

  private readonly conversationService = inject(ConversationService);
  private readonly messageWs = inject(MessageWsService);
  private readonly authState = inject(AuthState);

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

  private readonly currentUserId = computed(
    () => this.authState.user()?.id
  );

  constructor() {
    this.messageWs.watchMessages().subscribe(msg => {
      this.onIncomingMessage(msg);
    });
  }

private onIncomingMessage(incoming: MessageWsDto): void {
  this.conversations.update(list => {

    if (!list.some(c => c.bookingId === incoming.bookingId)) {
      this.refresh();
      return list;
    }

    return list.map(c => {
      if (c.bookingId !== incoming.bookingId) {
        return c;
      }

      const isMine = incoming.senderId === this.currentUserId();

      return {
        ...c,
        lastMessagePreview: incoming.messageContent,
        lastMessageAt: incoming.sentAt,
        unreadCount: isMine ? c.unreadCount : c.unreadCount + 1
      };
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
  markAsRead(conversation: ConversationSummary): void {
    if (conversation.unreadCount === 0) return;

    this.conversationService
      .markConversationAsRead(conversation.bookingId)
      .subscribe({
        next: () => {
          this.conversations.update(list =>
            list.map(c =>
              c.bookingId === conversation.bookingId
                ? { ...c, unreadCount: 0 }
                : c
            )
          );
        }
      });
  }

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


  clear(): void {
    this.conversations.set([]);
    this.errorMessage.set(null);
    this.loading.set(false);
  }
}
