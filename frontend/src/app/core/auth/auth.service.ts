import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, map, catchError, of, switchMap, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthState } from './auth.state';
import { AuthUser, RegisterRequest } from './auth.models';
import { getDeviceId, getSessionId } from './session.util';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthState);
  private readonly api = environment.apiBaseUrl;

  // =====================
  // LOGIN
  // =====================
  login(email: string, password: string): Observable<void> {
    return this.http.post<{ accessToken: string }>(
      `${this.api}/auth/login`,
      {
        email,
        password,
        deviceId: getDeviceId(),
        sessionId: getSessionId()
      },
      { withCredentials: true }
    ).pipe(
      tap(res => this.authState.setAccessToken(res.accessToken)),
      map(() => void 0)
    );
  }

  // =====================
  // REGISTER
  // =====================
  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(
      `${this.api}/auth/register`,
      request
    );
  }

  // =====================
  // FETCH CURRENT USER
  // =====================
  fetchMe(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${this.api}/users/me`).pipe(
      tap(user => {
        const token = this.authState.accessToken();
        if (token) {
          this.authState.setSession(user, token);
        }
      })
    );
  }

  // =====================
  // REFRESH ACCESS TOKEN
  // =====================
  refreshAccessToken(): Observable<string | null> {
    return this.http.post<{ accessToken: string }>(
      `${this.api}/auth/refresh`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(res => this.authState.setAccessToken(res.accessToken)),
      map(res => res.accessToken),
      catchError(() => of(null))
    );
  }

  // =====================
  // RESTORE SESSION
  // =====================
  restoreSession(): Observable<AuthUser | null> {
    return this.refreshAccessToken().pipe(
      switchMap(token => {
        if (!token) {
          this.authState.clear();
          return of(null);
        }
        return this.fetchMe();
      }),
      catchError(() => {
        this.authState.clear();
        return of(null);
      })
    );
  }

  // =====================
  // LOGOUT
  // =====================
  logout(): Observable<void> {
    return this.http.post(
      `${this.api}/auth/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(() => this.authState.clear()),
      map(() => void 0)
    );
  }

  // =====================
  // PASSWORD RESET
  // =====================
  forgotPassword(email: string) {
    return this.http.post(
      `${this.api}/auth/forgot-password`,
      { email }
    );
  }

  // =====================
  // PASSWORD RESET
  // =====================
  resetPassword(token: string, newPassword: string) {
    return this.http.post(
      `${this.api}/auth/reset-password`,
      { token, newPassword }
    );
  }

  // =====================
  // EMAIL VERIFICATION
  // =====================
  verifyEmail(token: string) {
    return this.http.get(
      `${this.api}/auth/verify-email`,
      { params: { token } }
    );
  }

  updateCachedUser(user: AuthUser): void {
  this.authState.setUserInternal(user);
}

}
