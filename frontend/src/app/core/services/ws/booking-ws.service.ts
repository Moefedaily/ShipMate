import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WsService } from './ws.service';
import { BookingStatusUpdateWsDto, ShipmentUpdateWsDto } from './ws.models';

@Injectable({ providedIn: 'root' })
export class BookingWsService {

  private readonly ws = inject(WsService);


  watchBooking(bookingId: string): Observable<BookingStatusUpdateWsDto> {
    return this.ws.subscribe<BookingStatusUpdateWsDto>(
      `/topic/bookings/${bookingId}`
    );
  }
  watchShipment(shipmentId: string) {
    return this.ws.subscribe<ShipmentUpdateWsDto>(
      `/topic/shipments/${shipmentId}`
    );
  }
}
