import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { WsService } from './ws.service';
import { DeliveryCodeWsDto } from './ws.models';

@Injectable({ providedIn: 'root' })
export class DeliveryCodeWsService {

  private readonly ws = inject(WsService);

  watchMyDeliveryCode(): Observable<DeliveryCodeWsDto> {
      console.log('SUBSCRIBE delivery-code');
    return this.ws.subscribe<DeliveryCodeWsDto>(
      `/user/queue/delivery-code`
    );
  }
}
