import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { inject, Injectable } from '@angular/core';
import { DriverEarningsSummaryResponse } from './earning.model';
import { PageResponse } from '../shipment/shipment.models';
import { DriverEarningResponse } from './earning.model';

@Injectable({ providedIn: 'root' })
export class EarningService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  getSummary() {
    return this.http.get<DriverEarningsSummaryResponse>(
      `${this.api}/driver/earnings/summary`
    );
  }

  getEarnings(page = 0, size = 20) {
    return this.http.get<PageResponse<DriverEarningResponse>>(
      `${this.api}/driver/earnings`,
      { params: { page, size } as any }
    );
  }
}