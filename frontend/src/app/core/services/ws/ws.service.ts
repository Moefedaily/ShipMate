import { Injectable, inject, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AuthState } from '../../auth/auth.state';

@Injectable({ providedIn: 'root' })
export class WsService {
  private readonly authState = inject(AuthState);

  private client: Client | null = null;

  private readonly _connected = signal(false);
  readonly isConnected = this._connected.asReadonly();

  private readonly subjects = new Map<string, Subject<any>>();
  private readonly stompSubs = new Map<string, StompSubscription>();

  connect(): void {

    if (this.client) {
      return;
    }

    const token = this.authState.accessToken();
    if (!token) {
      return;
    }

    const client = new Client({
      brokerURL: `${environment.wsBaseUrl}/ws`,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {}
    });

    client.onConnect = () => {
      this._connected.set(true);

      for (const [destination, subject] of this.subjects.entries()) {
        if (!this.stompSubs.has(destination)) {
          const sub = client.subscribe(destination, msg => {
            subject!.next(this.parse(msg));
          });
          this.stompSubs.set(destination, sub);
        }
      }
    };

    client.onDisconnect = () => {
      this._connected.set(false);
    };

    client.onWebSocketClose = () => {
      this._connected.set(false);
    };

    client.onWebSocketError = (e) => {
      this._connected.set(false);
    };

    client.onStompError = (frame) => {
      this._connected.set(false);
    };

    this.client = client;
    client.activate();
  }

  disconnect(): void {
    this._connected.set(false);

    for (const sub of this.stompSubs.values()) sub.unsubscribe();
    this.stompSubs.clear();

    this.client?.deactivate();
    this.client = null;
  }

  subscribe<T>(destination: string): Observable<T> {
    let subject = this.subjects.get(destination);

    if (!subject) {
      subject = new Subject<T>();
      this.subjects.set(destination, subject);

      if (this.client && this._connected()) {
        const sub = this.client.subscribe(destination, msg => subject!.next(this.parse(msg)));
        this.stompSubs.set(destination, sub);
      } else {
      }
    }

    return subject.asObservable();
  }

  private parse(message: IMessage) {
    try {
      return JSON.parse(message.body);
    } catch {
      return message.body;
    }
  }
}
