import { computed, Injectable, signal } from '@angular/core';
import { AuthUser } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthState {

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);

  private readonly _tokenVersion = signal(0);

  readonly user = this._user.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();

  readonly isAuthenticated = computed(
    () => this._user() !== null && this._accessToken() !== null
  );

  readonly isDriver = computed(
    () => this._user()?.userType === 'DRIVER' || this._user()?.userType === 'BOTH'
  );

  readonly isSender = computed(
    () => this._user()?.userType === 'SENDER' || this._user()?.userType === 'BOTH'
  );
  readonly tokenVersion = computed(() => this._tokenVersion());

  setSession(user: AuthUser, accessToken: string): void {
    this._user.set(user);
    this._accessToken.set(accessToken);
  }

  setAccessToken(accessToken: string): void {
    const previous = this._accessToken();
    this._accessToken.set(accessToken);
      if (previous !== accessToken) {
      this._tokenVersion.update(v => v + 1);
    }
  }

  clear(): void {
    this._user.set(null);
    this._accessToken.set(null);
    this._tokenVersion.update(v => v + 1);
  }

  setUserInternal(user: AuthUser): void {
    this._user.set(user);
  }

}
