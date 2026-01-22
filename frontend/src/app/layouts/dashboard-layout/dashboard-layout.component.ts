import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';


import {
  DashboardHeaderComponent,
  ActiveRole,
  UserType
} from '../../shared/components/dashboard-header/dashboard-header.component';

import { AuthState } from '../../core/auth/auth.state';

@Component({
  standalone: true,
  selector: 'app-dashboard-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    DashboardHeaderComponent,
    FooterComponent,
  ],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.scss'
})
export class DashboardLayoutComponent implements OnInit {

  private readonly router = inject(Router);
  private readonly authState = inject(AuthState);

  readonly userName = signal<string | undefined>(undefined);
  readonly userType = signal<UserType | undefined>(undefined);
  readonly activeRole = signal<ActiveRole>('SENDER');

  ngOnInit(): void {
    const user = this.authState.user();
    if (!user) return;

    this.userName.set(user.email);
    this.userType.set(user.userType);

    // Sync derive role from URL
    this.syncRoleWithUrl();

    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.syncRoleWithUrl());
  }

  private syncRoleWithUrl(): void {
    const url = this.router.url;

    if (url.includes('/dashboard/driver')) {
      this.activeRole.set('DRIVER');
    } else {
      this.activeRole.set('SENDER');
    }
  }

  onRoleChange(role: ActiveRole): void {
    this.router.navigate(['/dashboard', role.toLowerCase()]);
  }
}
