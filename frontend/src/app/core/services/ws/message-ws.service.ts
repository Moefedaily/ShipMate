import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WsService } from './ws.service';
import { MessageWsDto } from './ws.models';

@Injectable({ providedIn: 'root' })
export class MessageWsService {

  private readonly ws = inject(WsService);

  watchMessages(): Observable<MessageWsDto> {
    return this.ws.subscribe<MessageWsDto>(
      '/user/queue/messages'
    );
  }

  watchBookingMessages(bookingId: string): Observable<MessageWsDto> {
    return this.ws.subscribe<MessageWsDto>(
      `/topic/bookings/${bookingId}/messages`
    );
  }
}
