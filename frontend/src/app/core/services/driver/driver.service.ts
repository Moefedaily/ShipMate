import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';

import { DriverApplyRequest, DriverProfileResponse, UpdateLicenseRequest } from './driver.models';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DriverService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;


  getMyDriverProfile(): Observable<DriverProfileResponse> {
  return this.http.get<DriverProfileResponse>(
    `${this.api}/drivers/me`
  ).pipe(
    tap(driverProfile => {
      console.log(
        `[DRIVER ME] lat=${driverProfile.lastLatitude}, lng=${driverProfile.lastLongitude}`
      );
    }),
    catchError(err => {
      if (err.status === 404) {
        return throwError(() => err);
      }
      return throwError(() => err);
    })
  );
}


  applyAsDriver(request: DriverApplyRequest): Observable<DriverProfileResponse> {
    return this.http.post<DriverProfileResponse>(
      `${this.api}/drivers/apply`,
      request
    );
  }

  applyAsDriverWithPhotos(request: DriverApplyRequest, files: File[]): Observable<DriverProfileResponse> {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file));

    return this.http.post<DriverProfileResponse>(
      `${this.api}/drivers/apply`,
      formData
    );
  }

  updateLicense(request: UpdateLicenseRequest): Observable<DriverProfileResponse> {
    return this.http.put<DriverProfileResponse>(
      `${this.api}/drivers/me/license`,
      request
    );
  }

  uploadLicensePhotos(files: File[]): Observable<DriverProfileResponse> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));

    return this.http.post<DriverProfileResponse>(
      `${this.api}/drivers/me/license/photos`,
      formData
    );
  }
}
