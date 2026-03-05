import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { buildHttpParams } from '../../../shared/utils/query-params/query-params.util';

import {
  AdminDashboardStats,
  AdminUser,
  AdminShipment,
  AdminClaim,
  AdminPayment,
  PageResponse,

  UserFilterParams,
  ShipmentFilterParams,
  ClaimFilterParams,
  PaymentFilterParams,
  DriverFilterParams,

  ShipmentStatus,
  UserShipmentRow,
  UserClaimRow,
  UserPaymentRow,

  AdminDriverProfile,
  AdminStrikeRequest,
  AdminUpdateShipmentStatusRequest,
  AdminClaimDecisionRequest,
  AdminBooking,
  BookingFilterParams,
  AdminEarning,
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiBaseUrl}/admin`;

  /* =========================
     DASHBOARD
  ========================= */

  getDashboardStats(): Observable<AdminDashboardStats> {
    return this.http.get<AdminDashboardStats>(`${this.api}/dashboard`);
  }

  /* =========================
     USERS
  ========================= */

  getUsers(filters: UserFilterParams = {}): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(
      `${this.api}/users`,
      { params: buildHttpParams(filters) }
    );
  }

  getUserById(id: string): Observable<AdminUser> {
    return this.http.get<AdminUser>(`${this.api}/users/${id}`);
  }

  activateUser(id: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/users/${id}/activate`, {});
  }

  deactivateUser(id: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/users/${id}/deactivate`, {});
  }

  getUserShipments(userId: string, page = 0, size = 10): Observable<PageResponse<UserShipmentRow>> {
    return this.http.get<PageResponse<UserShipmentRow>>(
      `${this.api}/users/${userId}/shipments`,
      { params: buildHttpParams({ page, size }) }
    );
  }

  getUserClaims(userId: string, page = 0, size = 10): Observable<PageResponse<UserClaimRow>> {
    return this.http.get<PageResponse<UserClaimRow>>(
      `${this.api}/users/${userId}/claims`,
      { params: buildHttpParams({ page, size }) }
    );
  }

  getUserPayments(userId: string, page = 0, size = 10): Observable<PageResponse<UserPaymentRow>> {
    return this.http.get<PageResponse<UserPaymentRow>>(
      `${this.api}/users/${userId}/payments`,
      { params: buildHttpParams({ page, size }) }
    );
  }

  /* =========================
     SHIPMENTS
  ========================= */

  getShipments(filters: ShipmentFilterParams = {}): Observable<PageResponse<AdminShipment>> {
    return this.http.get<PageResponse<AdminShipment>>(
      `${this.api}/shipments`,
      { params: buildHttpParams(filters) }
    );
  }

  getShipmentById(id: string): Observable<AdminShipment> {
    return this.http.get<AdminShipment>(`${this.api}/shipments/${id}`);
  }

  updateShipmentStatus(
    id: string,
    status: ShipmentStatus,
    adminNotes?: string
  ): Observable<AdminShipment> {
    const body: AdminUpdateShipmentStatusRequest = { status, adminNotes };
    return this.http.patch<AdminShipment>(`${this.api}/shipments/${id}/status`, body);
  }

  /* =========================
     CLAIMS
  ========================= */

  getClaims(filters: ClaimFilterParams = {}): Observable<PageResponse<AdminClaim>> {
    return this.http.get<PageResponse<AdminClaim>>(
      `${this.api}/insurance/claims`,
      { params: buildHttpParams(filters) }
    );
  }

  getClaimById(id: string): Observable<AdminClaim> {
    return this.http.get<AdminClaim>(`${this.api}/insurance/claims/${id}`);
  }

  approveClaim(claimId: string, adminNotes?: string): Observable<AdminClaim> {
    const body: AdminClaimDecisionRequest = adminNotes ? { adminNotes } : {};
    return this.http.post<AdminClaim>(
      `${this.api}/insurance/claims/${claimId}/approve`,
      body
    );
  }

  rejectClaim(claimId: string, adminNotes?: string): Observable<AdminClaim> {
    const body: AdminClaimDecisionRequest = adminNotes ? { adminNotes } : {};
    return this.http.post<AdminClaim>(
      `${this.api}/insurance/claims/${claimId}/reject`,
      body
    );
  }

  /* =========================
     PAYMENTS
  ========================= */

  getPayments(filters: PaymentFilterParams = {}): Observable<PageResponse<AdminPayment>> {
    return this.http.get<PageResponse<AdminPayment>>(
      `${this.api}/payments`,
      { params: buildHttpParams(filters) }
    );
  }

  getPaymentById(id: string): Observable<AdminPayment> {
    return this.http.get<AdminPayment>(`${this.api}/payments/${id}`);
  }

  refundPayment(paymentId: string): Observable<void> {
    return this.http.post<void>(`${this.api}/payments/${paymentId}/refund`, {});
  }

  /* =========================
     DRIVERS
  ========================= */

  getDrivers(filters: DriverFilterParams = {}): Observable<PageResponse<AdminDriverProfile>> {
    return this.http.get<PageResponse<AdminDriverProfile>>(
      `${this.api}/drivers`,
      { params: buildHttpParams(filters) }
    );
  }

  getPendingDrivers(): Observable<AdminDriverProfile[]> {
    return this.http.get<AdminDriverProfile[]>(`${this.api}/drivers/pending`);
  }

  getDriversWithStrikes(): Observable<AdminDriverProfile[]> {
    return this.http.get<AdminDriverProfile[]>(`${this.api}/drivers/strikes`);
  }

  approveDriver(id: string): Observable<AdminDriverProfile> {
    return this.http.post<AdminDriverProfile>(`${this.api}/drivers/${id}/approve`, {});
  }

  rejectDriver(id: string): Observable<AdminDriverProfile> {
    return this.http.post<AdminDriverProfile>(`${this.api}/drivers/${id}/reject`, {});
  }

  suspendDriver(id: string): Observable<AdminDriverProfile> {
    return this.http.post<AdminDriverProfile>(`${this.api}/drivers/${id}/suspend`, {});
  }

  resetDriverStrikes(id: string): Observable<AdminDriverProfile> {
    return this.http.post<AdminDriverProfile>(`${this.api}/drivers/${id}/reset-strikes`, {});
  }

  addDriverStrike(id: string, note: string): Observable<AdminDriverProfile> {
    const body: AdminStrikeRequest = { note };
    return this.http.post<AdminDriverProfile>(`${this.api}/drivers/${id}/strikes`, body);
  }


  /* =========================
     BOOKINGS
  ========================= */
  getBookings(): Observable<AdminBooking[]> {
    return this.http.get<AdminBooking[]>(`${this.api}/bookings`);
  }

  getBookingById(id: string): Observable<AdminBooking> {
    return this.http.get<AdminBooking>(`${this.api}/bookings/${id}`);
  }
  /* =========================
     EARNINGS
  ========================= */
 getEarnings(page = 0, size = 10): Observable<PageResponse<AdminEarning>> {
    return this.http.get<PageResponse<AdminEarning>>(
      `${this.api}/earnings`,
      { params: buildHttpParams({ page, size }) }
    );
  }

  markEarningPaid(id: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/earnings/${id}/mark-paid`, {});
  }
}