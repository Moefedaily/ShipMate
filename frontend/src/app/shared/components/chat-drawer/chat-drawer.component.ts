import {
  Component,
  Input,
  Output,
  EventEmitter,
  inject,
  signal,
  computed,
  OnChanges
} from '@angular/core';
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
export class ChatDrawerComponent implements OnChanges {

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
    const status = this.selectedConversation()?.shipmentStatus;
    return status === 'DELIVERED' || status === 'CANCELLED';
  });

  ngOnChanges(): void {
    if (this.open) {
      this.conversationState.load();

      const list = this.conversations();
      if (list.length && !this.selectedConversation()) {
        this.selectConversation(list[0]);
      }
    }
  }

  selectConversation(convo: ConversationSummary): void {
    this.selectedConversation.set(convo);
  }

  requestClose(): void {
    this.selectedConversation.set(null);
    this.closeDrawer.emit();
  }

  getRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Now';
    if (diffMins < 60) return `${diffMins}m`;
    if (diffHours < 24) return `${diffHours}h`;
    if (diffDays < 7) return `${diffDays}d`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });
  }

  trackByShipment = (_: number, c: ConversationSummary) => c.shipmentId;
}