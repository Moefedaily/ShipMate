import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { NotificationResponse } from './notification.models';
import { PageResponse } from '../shipment/shipment.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  getMyNotifications(
    unreadOnly = false,
    page = 0,
    size = 20
  ): Observable<PageResponse<NotificationResponse>> {
    return this.http.get<PageResponse<NotificationResponse>>(
      `${this.api}/notifications/me`,
      {
        params: {
          unreadOnly,
          page,
          size
        }
      }
    );
  }

  markAllAsRead(): Observable<number> {
    return this.http.post<number>(
      `${this.api}/notifications/me/read-all`,
      {}
    );
  }

  markOneAsRead(notificationId: string): Observable<number> {
    return this.http.post<number>(
      `${this.api}/notifications/${notificationId}/read`,
      {}
    );
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(
      `${this.api}/notifications/me/unread-count`
    );
  }
}
