import { computed, Injectable, signal } from '@angular/core';
import { AuthUser } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthState {

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);

  // =====================
  // Public readonly state
  // =====================
  readonly user = this._user.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();

  // =====================
  // Derived state
  // =====================
  readonly isAuthenticated = computed(
    () => this._user() !== null && this._accessToken() !== null
  );

  readonly isDriver = computed(
    () => this._user()?.userType === 'DRIVER' || this._user()?.userType === 'BOTH'
  );

  readonly isSender = computed(
    () => this._user()?.userType === 'SENDER' || this._user()?.userType === 'BOTH'
  );

  // =====================
  // Mutations
  // =====================
  setSession(user: AuthUser, accessToken: string): void {
    this._user.set(user);
    this._accessToken.set(accessToken);
  }

  setAccessToken(accessToken: string): void {
    this._accessToken.set(accessToken);
  }

  clear(): void {
    this._user.set(null);
    this._accessToken.set(null);
  }

  // INTERNAL — used by AuthService
  setUserInternal(user: AuthUser): void {
    this._user.set(user);
  }

}
