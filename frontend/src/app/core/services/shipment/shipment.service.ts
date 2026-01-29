import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateShipmentRequest, ShipmentResponse } from './shipment.models';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ShipmentService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  create(request: CreateShipmentRequest): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(
      `${this.api}/shipments`,
      request
    );
  }

  uploadPhotos(shipmentId: string, files: File[]): Observable<void> {
    const formData = new FormData();
    files.forEach(f => formData.append('files', f));

    return this.http.post<void>(
      `${this.api}/shipments/${shipmentId}/photos`,
      formData
    );
  }
}
