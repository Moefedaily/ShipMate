import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DriverLocationService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  updateMyLocation(lat: number, lng: number) {
    console.log('[UPDATE LOCATION]', lat, lng);
    return this.http.post<void>(
      `${this.api}/drivers/me/location`,
      {
        latitude: lat,
        longitude: lng
      }
    );
  }
}
