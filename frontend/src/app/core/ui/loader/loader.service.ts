import { Injectable, computed, inject } from '@angular/core';
import { LoaderStore } from './loader.store';


@Injectable({ providedIn: 'root' })
export class LoaderService {

  private readonly store = inject(LoaderStore);

  readonly isLoading = computed(() => this.store.isLoading() > 0);

  show(): void {
    this.store.show();
  }

  hide(): void {
    this.store.hide();
  }

  reset(): void {
    this.store.reset();
  }
}
