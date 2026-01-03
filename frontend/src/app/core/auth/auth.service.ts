import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, switchMap, catchError, of, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthState } from './auth.state';
import { AuthUser, RegisterRequest } from './auth.models';
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
  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthTokensResponse>(`${this.api}/auth/login`, {
      email,
      password,
      deviceId: getDeviceId(),
      sessionId: getSessionId()
    }).pipe(
      tap(tokens => this.storeTokens(tokens)),
      switchMap(() => this.fetchMe())
    );
  }

  // =====================
  // REGISTER
  // =====================
  // =====================
// REGISTER
// =====================
register(request: RegisterRequest): Observable<void> {
  return this.http.post<void>(`${this.api}/auth/register`, request);
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
    if (!this.refreshToken) {
      return of(null);
    }

    return this.http
      .post<AuthTokensResponse>(`${this.api}/auth/refresh`, {
        refreshToken: this.refreshToken
      })
      .pipe(
        tap(tokens => this.storeTokens(tokens)),
        switchMap(tokens => of(tokens.accessToken))
      );
  }

  // =====================
  // RESTORE SESSION (APP BOOTSTRAP)
  // =====================
  restoreSession(): Observable<AuthUser | null> {
    return this.refreshAccessToken().pipe(
      switchMap(() => this.fetchMe()),
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  // =====================
  // LOGOUT
  // =====================
  logout(): Observable<null> {
    this.clearSession();
    return of(null);
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
