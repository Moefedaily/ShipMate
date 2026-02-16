import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { NotificationState } from '../../../core/state/notification/notification.state';
import { ClickOutsideDirective } from '../click-outside/click-outside.directive';
import { NotificationResponse } from '../../../core/services/notification/notification.models';

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

    // Handle navigation based on notification type
    // You can customize this based on your notification types
    // For now, we'll just close the panel
    this.close();
    
    // Example: Navigate based on type
    // if (notification.notificationType === 'SHIPMENT_ASSIGNED') {
    //   this.router.navigate(['/dashboard/shipments', notification.relatedId]);
    // }
  }

  viewAllNotifications(): void {
    this.close();
    // Navigate to a notifications page if you have one
    // this.router.navigate(['/notifications']);
  }

  
  getNotificationIcon(type: string): string {
    const iconMap: Record<string, string> = {
      'SHIPMENT_CREATED': 'add_circle',
      'SHIPMENT_ASSIGNED': 'assignment_ind',
      'SHIPMENT_IN_TRANSIT': 'local_shipping',
      'SHIPMENT_DELIVERED': 'check_circle',
      'SHIPMENT_CANCELLED': 'cancel',
      'BOOKING_CONFIRMED': 'event_available',
      'PAYMENT_RECEIVED': 'payments',
      'PAYMENT_FAILED': 'error',
      'MESSAGE_RECEIVED': 'message',
      'DRIVER_ARRIVED': 'location_on',
      'SYSTEM_UPDATE': 'info',
      'DEFAULT': 'notifications'
    };

    return iconMap[type] || iconMap['DEFAULT'];
  }

  getNotificationIconClass(type: string): string {
    const classMap: Record<string, string> = {
      'SHIPMENT_CREATED': 'icon-info',
      'SHIPMENT_ASSIGNED': 'icon-primary',
      'SHIPMENT_IN_TRANSIT': 'icon-warning',
      'SHIPMENT_DELIVERED': 'icon-success',
      'SHIPMENT_CANCELLED': 'icon-danger',
      'BOOKING_CONFIRMED': 'icon-success',
      'PAYMENT_RECEIVED': 'icon-success',
      'PAYMENT_FAILED': 'icon-danger',
      'MESSAGE_RECEIVED': 'icon-primary',
      'DRIVER_ARRIVED': 'icon-info',
      'SYSTEM_UPDATE': 'icon-info',
      'DEFAULT': 'icon-default'
    };

    return classMap[type] || classMap['DEFAULT'];
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