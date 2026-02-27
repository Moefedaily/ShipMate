import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';
import { CreateShipmentRequest, DeliveryCodeStatusResponse, PageResponse, PricingEstimateRequest, PricingEstimateResponse, ShipmentPricingPreviewRequest, ShipmentPricingPreviewResponse, ShipmentResponse, UpdateShipmentRequest } from './shipment.models';
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
  getMyShipments(page = 0, size = 10) {
    return this.http.get<PageResponse<ShipmentResponse>>(
      `${this.api}/shipments/me`,
      { params: { page, size, sort: 'createdAt,desc' } }
    );
  }

  getMyShipment(shipmentId: string) {
    return this.http.get<ShipmentResponse>(
      `${this.api}/shipments/${shipmentId}`
    );
  }
  updateShipment( id: string, request: UpdateShipmentRequest ): Observable<ShipmentResponse> {
    return this.http.put<ShipmentResponse>(
      `${this.api}/shipments/${id}`,
      request
    );
  }

  estimate(req: PricingEstimateRequest): Observable<PricingEstimateResponse> {
    return this.http.post<PricingEstimateResponse>(
      `${this.api}/pricing/estimate`,
      req
    );
  }
  markInTransit(shipmentId: string): any {
    return this.http.post(
      `${this.api}/shipments/${shipmentId}/in-transit`,
      {}
    );
  }

  markDelivered(shipmentId: string): any {
    return this.http.post(
      `${this.api}/shipments/${shipmentId}/confirm-delivery`,
      {}
    );
  }

  cancelShipment(shipmentId: string): any {
    return this.http.post(
      `${this.api}/shipments/${shipmentId}/cancel`,
      {}
    );
  }
  confirmDelivery(shipmentId: string, code: string) {
    return this.http.post<ShipmentResponse>(
      `${this.api}/shipments/${shipmentId}/confirm-delivery`,
      { code }
    );
  }

  getActiveDeliveryCode(shipmentId: string): Observable<DeliveryCodeStatusResponse | null> {
      return this.http.get<DeliveryCodeStatusResponse>(
        `${this.api}/shipments/${shipmentId}/delivery-code`,
        { observe: 'response' }
      ).pipe(
        map((res: HttpResponse<DeliveryCodeStatusResponse>) => {
          if (res.status === 204) return null;
          return res.body ?? null;
        })
      );
    }

    previewShipmentPricing( req: ShipmentPricingPreviewRequest ): Observable<ShipmentPricingPreviewResponse> {
    return this.http.post<ShipmentPricingPreviewResponse>(
      `${this.api}/pricing/shipment-preview`,
      req
    ).pipe(
      tap(res => console.log(res))
    );
  }
}


