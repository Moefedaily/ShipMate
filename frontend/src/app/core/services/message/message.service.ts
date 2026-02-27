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

  getShipmentMessages(
    shipmentId: string,
    page = 0,
    size = 50
  ): Observable<PageResponse<MessageResponse>> {
    return this.http.get<PageResponse<MessageResponse>>(
      `${this.api}/shipments/${shipmentId}/messages`,
      { params: { page, size } }
    );
  }

  sendMessage(
    shipmentId: string,
    message: string
  ): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.api}/shipments/${shipmentId}/messages`,
      { message }
    );
  }

  markAsRead(shipmentId: string): Observable<void> {
    return this.http.post<void>(
      `${this.api}/shipments/${shipmentId}/messages/read`,
      {}
    );
  }
}
