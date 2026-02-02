import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Observable, tap } from 'rxjs';
import { BookingResponse, CreateBookingRequest } from './booking.models';


@Injectable({ providedIn: 'root' })
export class BookingService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  createBooking(request: CreateBookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(
      `${this.api}/bookings`,
      request
    );
  }

  confirm(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(
      `${this.api}/bookings/${id}/confirm`,
      {}
    );
  }

  start(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(
      `${this.api}/bookings/${id}/start`,
      {}
    );
  }

  complete(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(
      `${this.api}/bookings/${id}/complete`,
      {}
    );
  }

  cancel(id: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(
      `${this.api}/bookings/${id}/cancel`,
      {}
    );
  }
  getMyActiveBooking() {
    return this.http.get<BookingResponse | null>(
      `${this.api}/bookings/me/active`
    ).pipe(
      tap(response => console.log('Active Booking:', response))
    );
  }

  getById(id: string): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(
      `${this.api}/bookings/${id}`
    );
  }

}
