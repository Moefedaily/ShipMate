import { Injectable, signal } from '@angular/core';
import { ToastMessage, ToastType } from './toast.models';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(type: ToastType, message: string, duration = 4000) {
    const toast: ToastMessage = {
      id: crypto.randomUUID(),
      type,
      message,
      duration
    };

    this._toasts.update(t => [...t, toast]);

    setTimeout(() => this.dismiss(toast.id), duration);
  }

  success(message: string) {
    this.show('success', message);
  }

  error(message: string) {
    this.show('error', message);
  }

  warning(message: string) {
    this.show('warning', message);
  }

  info(message: string) {
    this.show('info', message);
  }

  dismiss(id: string) {
    this._toasts.update(t => t.filter(x => x.id !== id));
  }
}
