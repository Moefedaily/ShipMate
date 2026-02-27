import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

import {
  InsuranceClaim,
  CreateInsuranceClaimRequest
} from './insurance.model';

import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class InsuranceService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  submitClaim(
    shipmentId: string,
    request: CreateInsuranceClaimRequest
  ): Observable<InsuranceClaim> {
    return this.http.post<InsuranceClaim>(
      `${this.api}/insurance/shipments/${shipmentId}`,
      request
    ).pipe(
      catchError(err => {
        return throwError(() => err);
      })
    );
  }

  addClaimPhotos( claimId: string, files: File[]): Observable<InsuranceClaim> {

    const formData = new FormData();

    files.forEach(file => {
        formData.append('files', file);
    });

    return this.http.post<InsuranceClaim>(
        `${this.api}/insurance/claims/${claimId}/photos`,
        formData
    );
    }

  getClaimByShipment(
    shipmentId: string
  ): Observable<InsuranceClaim> {
    return this.http.get<InsuranceClaim>(
      `${this.api}/insurance/shipments/${shipmentId}`
    ).pipe(
      catchError(err => {
        return throwError(() => err);
      })
    );
  }

  getMyClaims() {
    return this.http.get<InsuranceClaim[]>(
        `${this.api}/insurance/me`
    );
    }
}