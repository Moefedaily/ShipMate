import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ConversationSummary } from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  getMyConversations(): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(
      `${this.api}/conversations/me`
    );
  }
  markConversationAsRead(bookingId: string) {
    return this.http.post<void>(
        `${this.api}/messages/booking/${bookingId}/read`,
        {}
    );
 }

}
