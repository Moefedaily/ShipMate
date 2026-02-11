import { Component, Input, OnChanges, SimpleChanges, inject, signal, computed, ViewChild, ElementRef, AfterViewInit, effect} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MessageState } from '../../../core/state/message/message.state';

@Component({
  standalone: true,
  selector: 'app-chat',
  imports: [CommonModule],
  providers: [MessageState],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnChanges {

  @ViewChild('messagesContainer')
  private readonly messagesEl?: ElementRef<HTMLDivElement>;

  @Input({ required: true }) bookingId!: string;

  @Input() readonly = false;

  private readonly messageState = inject(MessageState);

  readonly messages = this.messageState.messages;
  readonly hasMessages = this.messageState.hasMessages;

  readonly draft = signal('');

  readonly canSend = computed(
    () => !this.readonly && this.draft().trim().length > 0
  );

  constructor() {
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scrollToBottom());
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['bookingId'] && this.bookingId) {
      this.messageState.loadForBooking(this.bookingId);
    }
  }

  send(): void {
    if (this.readonly) return;

    const text = this.draft().trim();
    if (!text) return;

    this.messageState.sendMessage(text);
    this.draft.set('');
  }
  private scrollToBottom(): void {
    const el = this.messagesEl?.nativeElement;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  }


  trackById = (_: number, msg: any) => msg.id;
}
