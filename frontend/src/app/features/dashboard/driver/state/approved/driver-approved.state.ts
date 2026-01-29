import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { MatchingService } from '../../../../../core/services/driver/matching/matching.service';
import { LoaderService } from '../../../../../core/ui/loader/loader.service';
import { ToastService } from '../../../../../core/ui/toast/toast.service';
import { MatchResult } from '../../../../../core/services/driver/matching/matching.models';
import { BookingResponse } from '../../../../../core/services/booking/booking.models';
import { BookingService } from '../../../../../core/services/booking/booking.service';


@Component({
  standalone: true,
  selector: 'app-driver-approved-state',
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './driver-approved.state.html',
  styleUrl: './driver-approved.state.scss'
})
export class DriverApprovedState implements OnInit {
  private readonly bookingService = inject(BookingService);
  private readonly matchingService = inject(MatchingService);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);


  readonly matches = signal<MatchResult[]>([]);
  readonly activeBooking = signal<BookingResponse | null>(null);

  ngOnInit(): void {
    this.loadPreview();
    this.loadActiveBooking();
  }

  private loadActiveBooking(): void {
    this.loader.show();

    this.bookingService.getMyActiveBooking().subscribe({
      next: booking => {
        this.activeBooking.set(booking);
        this.loader.hide();
      },
      error: () => {
        this.loader.hide();
      }
    });
  }
  private loadPreview(): void {
    this.loader.show();

    this.matchingService.getNearbyShipments().subscribe({
      next: res => {
        // Preview only (top 3)
        this.matches.set(res.slice(0, 3));
        this.loader.hide();
      },
      error: err => {
        this.loader.hide();
        this.toast.error(
          err?.error?.message || 'Unable to load deliveries'
        );
      }
    });
  }
}
