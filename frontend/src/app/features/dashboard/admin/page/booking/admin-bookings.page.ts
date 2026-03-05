import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AdminBooking } from '../../../../../core/services/admin/admin.models';
import { AdminService } from '../../../../../core/services/admin/admin.service';

@Component({
  selector: 'app-admin-bookings',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './admin-bookings.page.html',
  styleUrl: './admin-bookings.page.scss',
})
export class AdminBookingsPage implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly bookings = signal<AdminBooking[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getBookings().subscribe({
      next: data => { this.bookings.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load bookings'); this.loading.set(false); }
    });
  }

  shortId(id: string): string {
    return id ? id.slice(0, 8) + '…' : '—';
  }
}