import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  inject,
  signal,
  computed,
  ViewChild,
  ElementRef,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { MessageState } from '../../../core/state/message/message.state';
import { AuthState } from '../../../core/auth/auth.state';

@Component({
  standalone: true,
  selector: 'app-chat',
  imports: [CommonModule, MatIconModule],
  providers: [MessageState],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnChanges {
  
  @ViewChild('messagesContainer')
  private readonly messagesEl?: ElementRef<HTMLDivElement>;

  @Input({ required: true }) bookingId!: string;
  @Input() readonly = false;

  /* ==================== Inject ==================== */
  private readonly messageState = inject(MessageState);
  private readonly authState = inject(AuthState);

  /* ==================== Signals ==================== */
  readonly messages = this.messageState.messages;
  readonly hasMessages = this.messageState.hasMessages;
  readonly typingUser = this.messageState.typingUser;
  readonly draft = signal('');

  /* ==================== Computed ==================== */
  readonly canSend = computed(
    () => !this.readonly && this.draft().trim().length > 0
  );

  readonly currentUserId = computed(() => this.authState.user()?.id || '');

  /* ==================== Constructor ==================== */
  constructor() {
    // Auto-scroll when messages update
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scrollToBottom());
    });
  }

  /* ==================== Lifecycle ==================== */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['bookingId'] && this.bookingId) {
      this.messageState.loadForBooking(this.bookingId);
    }
  }

  /* ==================== Input Handling ==================== */
  onDraftInput(value: string): void {
    if (this.readonly) return;
    this.draft.set(value);
    
    if (this.bookingId) {
      this.messageState.sendTyping();
    }
  }

  /* ==================== Send Message ==================== */
  send(): void {
    if (this.readonly) return;
    
    const text = this.draft().trim();
    if (!text) return;

    this.messageState.sendMessage(text);
    this.draft.set('');
  }

  /* ==================== Helpers ==================== */
  private scrollToBottom(): void {
    const el = this.messagesEl?.nativeElement;
    if (!el) return;
    
    setTimeout(() => {
      el.scrollTop = el.scrollHeight;
    }, 50);
  }

  trackById = (_: number, msg: any) => msg.id;
}