import { Component, Input, Output, EventEmitter, inject, signal, computed, effect } from '@angular/core';

import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { ChatComponent } from '../chat/chat.component';
import { ConversationListState } from '../../../core/state/conversation/conversation-list.state';
import { ConversationSummary } from '../../../core/services/conversation/conversation.models';

@Component({
  standalone: true,
  selector: 'app-chat-drawer',
  imports: [
    CommonModule,
    ChatComponent,
    MatIconModule
  ],
  providers: [ConversationListState],
  templateUrl: './chat-drawer.component.html',
  styleUrl: './chat-drawer.component.scss'
})
export class ChatDrawerComponent {

  private readonly conversationState = inject(ConversationListState);


  @Input({ required: true }) open!: boolean;
  @Output() closeDrawer = new EventEmitter<void>();


  readonly conversations = this.conversationState.sortedConversations;
  readonly loading = this.conversationState.loading;
  readonly errorMessage = this.conversationState.errorMessage;

  readonly selectedConversation = signal<ConversationSummary | null>(null);

  readonly hasConversations = computed(
    () => this.conversations().length > 0
  );

  readonly isChatReadonly = computed(() => {
    const status = this.selectedConversation()?.bookingStatus;
    return status === 'COMPLETED' || status === 'CANCELLED';
  });


  constructor() {
    effect(() => {
      if (!this.open) return;

      this.conversationState.load();

      const list = this.conversations();
      if (!list.length) return;

      if (!this.selectedConversation()) {
        this.selectConversation(list[0]);
      }
    });
  }


  selectConversation(convo: ConversationSummary): void {
    this.selectedConversation.set(convo);
    this.conversationState.markAsRead(convo);
  }

  requestClose(): void {
    this.selectedConversation.set(null);
    this.closeDrawer.emit();
  }

  trackByBooking = (_: number, c: ConversationSummary) => c.bookingId;
}
