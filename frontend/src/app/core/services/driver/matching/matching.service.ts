import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { MatchResult } from './matching.models';

@Injectable({ providedIn: 'root' })
export class MatchingService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  /**
   * Fetch nearby shipments for an approved driver.
   * Location is resolved server-side (driver profile).
   */
  getNearbyShipments(options?: {
    radiusKm?: number;
    maxResults?: number;
  }): Observable<MatchResult[]> {

    let params = new HttpParams();

    if (options?.radiusKm) {
      params = params.set('radiusKm', options.radiusKm);
    }

    if (options?.maxResults) {
      params = params.set('maxResults', options.maxResults);
    }

    return this.http.get<MatchResult[]>(
      `${this.api}/matching/shipments`,
      { params }
    );
  }
}
