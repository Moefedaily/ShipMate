import { Component, computed, signal } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { AdminSidebarComponent } from '../../features/dashboard/admin/components/admin-sidebar/admin-sidebar.component';
import { AdminHeaderComponent } from '../../features/dashboard/admin/components/admin-header/admin-header.component';


const ROUTE_TITLES: Record<string, string> = {
  '/admin':           'Overview',
  '/admin/users':     'Users',
  '/admin/drivers':   'Drivers',
  '/admin/shipments': 'Shipments',
  '/admin/bookings':  'Bookings',
  '/admin/claims':    'Claims',
  '/admin/payments':  'Payments',
  '/admin/earnings':  'Earnings',
};

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AdminSidebarComponent, AdminHeaderComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  readonly sidebarExpanded = signal(false);
  readonly pageTitle       = signal('Overview');

  constructor(private router: Router) {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        const url = e.urlAfterRedirects.split('?')[0];
        const exact = ROUTE_TITLES[url];
        if (exact) {
          this.pageTitle.set(exact);
          return;
        }
        const prefix = Object.keys(ROUTE_TITLES)
          .filter(k => url.startsWith(k) && k !== '/admin')
          .sort((a, b) => b.length - a.length)[0];
        this.pageTitle.set(prefix ? ROUTE_TITLES[prefix] : 'Admin');
      });
  }

  onSidebarToggle(expanded: boolean): void {
    this.sidebarExpanded.set(expanded);
  }
}