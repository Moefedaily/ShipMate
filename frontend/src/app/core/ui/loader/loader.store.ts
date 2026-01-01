import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoaderStore {

  private readonly _loadingCount = signal(0);

  readonly isLoading = this._loadingCount.asReadonly();

  show(): void {
    this._loadingCount.update(v => v + 1);
  }

  hide(): void {
    this._loadingCount.update(v => Math.max(v - 1, 0));
  }

  reset(): void {
    this._loadingCount.set(0);
  }
}
