import { Injectable, signal, computed, inject } from '@angular/core';
import { Subscription } from 'rxjs';

import { NotificationWsService } from '../../services/ws/notification-ws.service';
import { NotificationService } from '../../services/notification/notification.service';

import { NotificationWsDto } from '../../services/ws/ws.models';
import { NotificationResponse } from '../../services/notification/notification.models';
import { AuthState } from '../../auth/auth.state';

type UiNotification = NotificationResponse;

@Injectable({ providedIn: 'root' })

export class NotificationState {
  private readonly notificationWs = inject(NotificationWsService);
  private readonly notificationService = inject(NotificationService);

  private readonly authState = inject(AuthState);

  private notifSub?: Subscription;
  private countSub?: Subscription;

  private historyLoaded = false;

  readonly notifications = signal<UiNotification[]>([]);
  readonly unreadCount = signal(0);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);

  readonly hasUnread = computed(() => this.unreadCount() > 0);

  init(): void {
    const userId = this.authState.user()?.id;
    if (!userId) return;

    this.startListening(userId);
    this.loadUnreadCount();
  }

  private startListening(userId: string): void {
    if (!this.countSub) {
      this.countSub = this.notificationWs.watchUnreadCount(userId).subscribe({
        next: payload => {
          this.unreadCount.set(payload.unreadCount);
        },
        error: () => {
          this.errorMessage.set('Notification count stream error');
        }
      });
    }

    if (!this.notifSub) {
      this.notifSub = this.notificationWs.watchNotifications(userId).subscribe({
        next: notif => {
          this.onWsNotification(notif);
        },
        error: () => {
          this.errorMessage.set('Notification stream error');
        }
      });
    }
  }

  private onWsNotification(ws: NotificationWsDto): void {

    const exists = this.notifications().some(n => n.id === ws.id);
    if (exists) return;

    const item: UiNotification = {
      id: ws.id,
      title: ws.title,
      message: ws.message,
      notificationType: ws.type,
      referenceId: ws.referenceId ?? null,
      referenceType: ws.referenceType ?? null,
      isRead: false,
      createdAt: ws.createdAt
    };

    this.notifications.update(list => [item, ...list]);

    this.unreadCount.update(count => count + 1);
  }


  private loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: count => this.unreadCount.set(count),
      error: () => this.errorMessage.set('Failed to load unread count')
    });
  }

  loadHistory(): void {
    if (this.historyLoaded || this.loading()) return;

    this.loading.set(true);

    this.notificationService.getMyNotifications(false, 0, 50).subscribe({
      next: page => {
        this.notifications.set(page.content);
        this.historyLoaded = true;
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load notifications');
        this.loading.set(false);
      }
    });
  }

  markAllAsRead(): void {
    if (!this.hasUnread()) return;

    this.notificationService.markAllAsRead().subscribe({
      next: unreadCount => {
        this.unreadCount.set(unreadCount);
        this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
      },
      error: () => this.errorMessage.set('Failed to mark notifications as read')
    });
  }

  markOneAsRead(notificationId: string): void {
    this.notificationService.markOneAsRead(notificationId).subscribe({
      next: unreadCount => {
        this.unreadCount.set(unreadCount);
        this.notifications.update(list =>
          list.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
        );
      },
      error: () => this.errorMessage.set('Failed to mark notification as read')
    });
  }

  clear(): void {
    this.notifSub?.unsubscribe();
    this.notifSub = undefined;

    this.countSub?.unsubscribe();
    this.countSub = undefined;

    this.notifications.set([]);
    this.unreadCount.set(0);
    this.historyLoaded = false;
    this.errorMessage.set(null);
  }
}
