import { Injectable, computed, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoaderService {
  private readonly _activeRequests = signal(0);

  readonly loading = computed(() => this._activeRequests() > 0);

  show() {
    this._activeRequests.update(v => v + 1);
  }

  hide() {
    this._activeRequests.update(v => Math.max(0, v - 1));
  }

  reset() {
    this._activeRequests.set(0);
  }
}
