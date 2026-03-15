import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Vehicle } from './vehicle.models';

@Injectable({ providedIn: 'root' })
export class VehicleService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  // --- DRIVER ACTIONS ---

  addVehicle(vehicle: Partial<Vehicle>): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${this.api}/vehicles`, vehicle);
  }

  getMyVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(`${this.api}/vehicles/mine`);
  }

  activateVehicle(vehicleId: string): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.api}/vehicles/${vehicleId}/activate`, {});
  }
}
