import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { NotificationState } from '../../../core/state/notification/notification.state';
import { ClickOutsideDirective } from '../click-outside/click-outside.directive';

@Component({
  standalone: true,
  selector: 'app-notifications',
  imports: [CommonModule, MatIconModule,ClickOutsideDirective],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent {

  private readonly state = inject(NotificationState);

  readonly notifications = this.state.notifications;
  readonly unreadCount = this.state.unreadCount;
  readonly hasUnread = this.state.hasUnread;

  readonly isOpen = signal(false);

  toggle(): void {
    const next = !this.isOpen();

    if (next) {
      this.open();
    } else {
      this.close();
    }
  }

  private open(): void {
    if (this.isOpen()) return;

    this.isOpen.set(true);
    this.state.loadHistory();
  }

  close(): void {
    if (!this.isOpen()) return;

    this.isOpen.set(false);

    if (this.hasUnread()) {
      this.state.markAllAsRead();
    }
  }
}
