import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { MessageResponse } from './message.models';
import { PageResponse } from '../shipment/shipment.models';

@Injectable({ providedIn: 'root' })
export class MessageService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  getBookingMessages(
    bookingId: string,
    page = 0,
    size = 50
  ): Observable<PageResponse<MessageResponse>> {
    return this.http.get<PageResponse<MessageResponse>>(
      `${this.api}/bookings/${bookingId}/messages`,
      { params: { page, size } }
    );
  }

  sendMessage(
    bookingId: string,
    message: string
  ): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.api}/bookings/${bookingId}/messages`,
      { message }
    );
  }

  markAsRead(bookingId: string): Observable<void> {
    return this.http.post<void>(
      `${this.api}/bookings/${bookingId}/messages/read`,
      {}
    );
  }
}
