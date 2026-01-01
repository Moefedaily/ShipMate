import { Injectable, signal, computed } from '@angular/core';
import { AuthUser } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthState {

  private readonly _user = signal<AuthUser | null>(null);

  // =====================
  // Public signals
  // =====================

  readonly user = this._user.asReadonly();

  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isDriver = computed(() => this._user()?.userType === 'DRIVER');
  readonly isSender = computed(() => this._user()?.userType === 'SENDER');

  // =====================
  // Mutations (AuthService only)
  // =====================

  setUser(user: AuthUser): void {
    this._user.set(user);
  }

  clear(): void {
    this._user.set(null);
  }
}
