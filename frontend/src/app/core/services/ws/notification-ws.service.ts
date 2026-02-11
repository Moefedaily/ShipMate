import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WsService } from './ws.service';
import { NotificationWsDto, UnreadCountWsDto } from './ws.models';

@Injectable({ providedIn: 'root' })
export class NotificationWsService {

  private readonly ws = inject(WsService);

 watchNotifications(userId: string): Observable<NotificationWsDto> {
    return this.ws.subscribe<NotificationWsDto>(
      `/topic/users/${userId}/notifications`
    );
  }

  watchUnreadCount(userId: string): Observable<UnreadCountWsDto> {
    return this.ws.subscribe<UnreadCountWsDto>(
      `/topic/users/${userId}/notifications/unread-count`
    );
  }

}
