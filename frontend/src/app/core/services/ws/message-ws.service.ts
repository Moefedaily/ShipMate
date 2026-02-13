import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WsService } from './ws.service';
import { ConversationUpdateWsDto, MessageWsDto, TypingWsDto } from './ws.models';
import { MessageResponse } from '../message/message.models';

@Injectable({ providedIn: 'root' })
export class MessageWsService {

  private readonly ws = inject(WsService);

  watchConversationUpdates(userId: string) {
  return this.ws.subscribe<ConversationUpdateWsDto>(
    `/topic/users/${userId}/conversation-updates`
  );
}

  watchBookingMessages(bookingId: string): Observable<MessageResponse> {
    return this.ws.subscribe<MessageResponse>(
      `/topic/bookings/${bookingId}/messages`
    );
  }

  sendTyping(bookingId: string) {
    this.ws.publish(
      `/app/bookings/${bookingId}/typing`,
      {}
    );
  }

  watchTyping(bookingId: string) {
    return this.ws.subscribe<TypingWsDto>(
      `/topic/bookings/${bookingId}/typing`
    );
  }

}
