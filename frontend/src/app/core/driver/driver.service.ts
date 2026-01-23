import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DriverApplyRequest, DriverProfileResponse } from './driver.models';

@Injectable({ providedIn: 'root' })
export class DriverService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  /**
   * Returns the driver profile of the authenticated user.
   * 404 = not applied
   */
  getMyDriverProfile(): Observable<DriverProfileResponse> {
    return this.http.get<DriverProfileResponse>(
      `${this.api}/drivers/me`
    );
  }

  applyAsDriver( request: DriverApplyRequest ): Observable<DriverProfileResponse> {
    return this.http.post<DriverProfileResponse>(
    `${this.api}/drivers/apply`,
    request
  );
}

}
