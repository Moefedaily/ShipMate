import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreatePaymentIntentResponse,
  PaymentResponse
} from './payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  createPaymentIntent(shipmentId: string): Observable<CreatePaymentIntentResponse> {
    return this.http.post<CreatePaymentIntentResponse>(
      `${this.api}/shipments/${shipmentId}/payment/intent`,
      {}
    );
  }

  getPayment(shipmentId: string): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(
      `${this.api}/shipments/${shipmentId}/payment`
    );
  }
}
