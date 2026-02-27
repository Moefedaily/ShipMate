import { Injectable, signal } from '@angular/core';
import { ToastMessage, ToastType } from './toast.models';

@Injectable({ providedIn: 'root' })
export class ToastService {

  private readonly _messages = signal<ToastMessage[]>([]);
  private counter = 0;

  readonly messages = this._messages.asReadonly();

  // =====================
  // Public API
  // =====================

  success(message: string): void {
    this.add('success', message);
  }

  error(message: string): void {
    this.add('error', message);
  }

  info(message: string): void {
    this.add('info', message);
  }

  warning(message: string): void {
    this.add('warning', message);
  }

  remove(id: number): void {
    this._messages.update(list =>
      list.filter(msg => msg.id !== id)
    );
  }

  clear(): void {
    this._messages.set([]);
  }

  // =====================
  // Internal
  // =====================

  private add(type: ToastType, message: string): void {
    const toast: ToastMessage = {
      id: ++this.counter,
      type,
      message
    };

    this._messages.update(list => [...list, toast]);

    setTimeout(() => this.remove(toast.id), 5000);
  }
}
