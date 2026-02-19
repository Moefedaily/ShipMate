import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WsService } from './ws.service';
import { ConversationUpdateWsDto, TypingWsDto } from './ws.models';
import { MessageResponse } from '../message/message.models';

@Injectable({ providedIn: 'root' })
export class MessageWsService {

  private readonly ws = inject(WsService);

  watchConversationUpdates(userId: string) {
    return this.ws.subscribe<ConversationUpdateWsDto>(
      `/topic/users/${userId}/conversation-updates`
    );
  }

  watchShipmentMessages(shipmentId: string): Observable<MessageResponse> {
    return this.ws.subscribe<MessageResponse>(
      `/topic/shipments/${shipmentId}/messages`
    );
  }

  sendTyping(shipmentId: string) {
    this.ws.publish(
      `/app/shipments/${shipmentId}/typing`,
      {}
    );
  }

  watchTyping(shipmentId: string) {
    return this.ws.subscribe<TypingWsDto>(
      `/topic/shipments/${shipmentId}/typing`
    );
  }
}
