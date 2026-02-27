import { Injectable, signal, computed, inject, effect } from '@angular/core';
import { catchError, of, Observable, Subscription, tap, finalize } from 'rxjs';
import { BookingService } from '../../services/booking/booking.service';
import { BookingWsService } from '../../services/ws/booking-ws.service';
import { BookingResponse } from '../../services/booking/booking.models';

type BookingAction = 'confirm' | 'start' | 'complete' | 'cancel';

@Injectable()
export class BookingState {
  private readonly bookingService = inject(BookingService);
  private readonly bookingWs = inject(BookingWsService);
  
  private bookingUpdatesSub?: Subscription;
  private currentBookingId?: string;
  
  readonly booking = signal<BookingResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);
  readonly status = computed(() => this.booking()?.status);

  constructor() {
    effect(() => {
      const bookingId = this.booking()?.id;
      if (!bookingId) {
        this.clearUpdates();
        return;
      }
      this.listenToUpdates(bookingId);
    });
  }

  load(id: string): void {
    this.errorMessage.set(null);
    this.loading.set(true);
    
    this.bookingService.getById(id).pipe(
      catchError(err => {
        this.errorMessage.set(
          err.error?.message || 'Unable to load booking'
        );
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe(booking => {
      this.booking.set(booking);
    });
  }

  runAction(action: BookingAction): Observable<BookingResponse | null> {
    const booking = this.booking();
    if (!booking) {
      return of(null);
    }

    const call$ = (() => {
      switch (action) {
        case 'confirm': return this.bookingService.confirm(booking.id);
        case 'start': return this.bookingService.start(booking.id);
        case 'complete': return this.bookingService.complete(booking.id);
        case 'cancel': return this.bookingService.cancel(booking.id);
      }
    })();

    return call$.pipe(
      tap(updated => {
        if (updated) {
          this.booking.set(updated);
        }
      }),
      catchError(err => {
        this.errorMessage.set(
          err.error?.message || 'Action failed'
        );
        return of(null);
      })
    );
  }

  private clearUpdates(): void {
    this.bookingUpdatesSub?.unsubscribe();
    this.bookingUpdatesSub = undefined;
    this.currentBookingId = undefined;
  }

  private listenToUpdates(bookingId: string): void {
    if (this.currentBookingId === bookingId) {
      return;
    }

    this.currentBookingId = bookingId;
    this.bookingUpdatesSub?.unsubscribe();
    
    this.bookingUpdatesSub =
      this.bookingWs.watchBooking(bookingId)
        .subscribe(update => {
          this.booking.update(b => {
            if (!b || b.status === update.status) return b;
            return { ...b, status: update.status };
          });
        });
  }
}