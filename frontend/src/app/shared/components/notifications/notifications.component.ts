import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { NotificationState } from '../../../core/state/notification/notification.state';
import { ClickOutsideDirective } from '../click-outside/click-outside.directive';
import { NotificationResponse } from '../../../core/services/notification/notification.models';
import { AuthState } from '../../../core/auth/auth.state';

@Component({
  standalone: true,
  selector: 'app-notifications',
  imports: [CommonModule, MatIconModule, ClickOutsideDirective],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent {
  
  private readonly state = inject(NotificationState);
  private readonly router = inject(Router);
  private readonly authState = inject(AuthState);

  readonly notifications = this.state.notifications;
  readonly unreadCount = this.state.unreadCount;
  readonly hasUnread = this.state.hasUnread;
  readonly loading = this.state.loading;
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
  }

  
  markAllRead(): void {
    this.state.markAllAsRead();
  }

  markOneRead(notificationId: string): void {
    this.state.markOneAsRead(notificationId);
  }

  
  handleNotificationClick(notification: NotificationResponse): void {

    if (!notification.isRead) {
      this.markOneRead(notification.id);
    }

    this.close();

    if (!notification.referenceId || !notification.referenceType) {
      return;
    }

    const roleType = this.authState.user()?.userType;

    if (
      notification.notificationType === 'PAYMENT_STATUS' &&
      notification.referenceType === 'SHIPMENT'
    ) {
      this.router.navigate([
        '/dashboard/shipments',
        notification.referenceId,
        'payment'
      ]);
      return;
    }

    switch (notification.referenceType) {

      case 'SHIPMENT':
        if (roleType === 'SENDER') {
          this.router.navigate([
            '/dashboard/shipments',
            notification.referenceId
          ]);
        }
        break;

      case 'BOOKING':
        if (roleType === 'DRIVER') {
          this.router.navigate([
            '/dashboard/trip',
            notification.referenceId
          ]);
        } else if (roleType === 'SENDER') {
          this.router.navigate([
            '/dashboard/shipments'
          ]);
        }
        break;

      case 'MESSAGE':
        this.router.navigate([
          '/dashboard/chat',
          notification.referenceId
        ]);
        break;

      default:
        break;
    }
  }

  
  getNotificationIcon(type: string): string {
    const iconMap: Record<string, string> = {
      'BOOKING_UPDATE': 'event',
      'PAYMENT_STATUS': 'payments',
      'DELIVERY_STATUS': 'local_shipping',
      'NEW_MESSAGE': 'chat',
      'SYSTEM_ALERT': 'info',
    };

    return iconMap[type] || 'notifications';
  }


  getNotificationIconClass(type: string): string {
    const classMap: Record<string, string> = {
      'BOOKING_UPDATE': 'icon-primary',
      'PAYMENT_STATUS': 'icon-success',
      'DELIVERY_STATUS': 'icon-warning',
      'NEW_MESSAGE': 'icon-info',
      'SYSTEM_ALERT': 'icon-neutral',
    };

    return classMap[type] || 'icon-default';
  }


  getRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}