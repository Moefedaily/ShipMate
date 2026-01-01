import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthState } from './auth.state';
import { AuthUser } from './auth.models';
import { environment } from '../../../environments/environment';
import { catchError, EMPTY, map, Observable, of, switchMap, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthState);

  private readonly api = environment.apiBaseUrl;

  // =====================
  // LOGIN
  // =====================

login(email: string, password: string) {
  return this.http
    .post<void>(`${this.api}/auth/login`, { email, password }, { withCredentials: true })
    .pipe(
      switchMap(() => this.fetchMe())
    );
}


// =====================
// ME
// =====================
fetchMe() {
  return this.http
    .get<AuthUser>(`${this.api}/users/me`, { withCredentials: true })
    .pipe(
      tap(user => this.authState.setUser(user))
    );
}

  // =====================
  // SESSION RESTORE
  // =====================

    restoreSession(): Observable<void> {
    return this.fetchMe().pipe(
        map(() => void 0),
        catchError(() => {
        this.authState.clear();
        return of(void 0);
        })
    );
    }

  // =====================
  // LOGOUT
  // =====================

  logout() {
    return this.http
      .post(`${this.api}/auth/logout`, {}, { withCredentials: true })
      .subscribe(() => {
        this.authState.clear();
      });
  }
}
