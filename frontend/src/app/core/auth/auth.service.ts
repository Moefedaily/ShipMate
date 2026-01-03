import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, map, catchError, of, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthState } from './auth.state';
import { RegisterRequest } from './auth.models';
import { getDeviceId, getSessionId } from './session.util';

interface AuthTokensResponse {
  accessToken: string;
  refreshToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthState);
  private readonly api = environment.apiBaseUrl;

  private refreshToken: string | null = null;

  // =====================
  // LOGIN
  // =====================
  login(email: string, password: string): Observable<void> {
    return this.http
      .post<AuthTokensResponse>(`${this.api}/auth/login`, {
        email,
        password,
        deviceId: getDeviceId(),
        sessionId: getSessionId()
      })
      .pipe(
        tap(tokens => this.storeTokens(tokens)),
        map(() => void 0)
      );
  }

  // =====================
  // REGISTER
  // =====================
  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.api}/auth/register`, request);
  }

  // =====================
  // FETCH CURRENT USER
  // =====================
  fetchMe(): Observable<any> {
    return this.http.get<any>(`${this.api}/users/me`).pipe(
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
    if (!this.refreshToken) {
      return of(null);
    }

    return this.http
      .post<AuthTokensResponse>(`${this.api}/auth/refresh`, {
        refreshToken: this.refreshToken
      })
      .pipe(
        tap(tokens => this.storeTokens(tokens)),
        map(tokens => tokens.accessToken),
        catchError(() => of(null))
      );
  }

  // =====================
  // RESTORE SESSION
  // =====================
  restoreSession(): Observable<void> {
    return this.refreshAccessToken().pipe(
      map(() => void 0),
      catchError(() => {
        this.clearSession();
        return of(void 0);
      })
    );
  }

  // =====================
  // LOGOUT
  // =====================
  logout(): Observable<void> {
    this.clearSession();
    return of(void 0);
  }

  // =====================
  // INTERNAL HELPERS
  // =====================
  private storeTokens(tokens: AuthTokensResponse): void {
    this.refreshToken = tokens.refreshToken;
    this.authState.setAccessToken(tokens.accessToken);
  }

  private clearSession(): void {
    this.refreshToken = null;
    this.authState.clear();
  }
}
