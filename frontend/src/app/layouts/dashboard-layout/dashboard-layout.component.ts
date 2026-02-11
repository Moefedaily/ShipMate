import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';

import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

import {
  DashboardHeaderComponent,
  ActiveRole,
  UserType
} from '../../shared/components/dashboard-header/dashboard-header.component';

import { FooterComponent } from '../../shared/components/footer/footer.component';
import { ChatDrawerComponent } from '../../shared/components/chat-drawer/chat-drawer.component';

import { AuthState } from '../../core/auth/auth.state';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationState } from '../../core/state/notification/notification.state';

@Component({
  standalone: true,
  selector: 'app-dashboard-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    DashboardHeaderComponent,
    FooterComponent,
    ChatDrawerComponent
  ],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.scss'
})
export class DashboardLayoutComponent implements OnInit {

  private readonly router = inject(Router);
  private readonly authState = inject(AuthState);
  private readonly authService = inject(AuthService);
  private readonly notificationState = inject(NotificationState);

  readonly userType = signal<UserType | undefined>(undefined);
  readonly activeRole = signal<ActiveRole>('SENDER');
  readonly chatOpen = signal(false);

  readonly userName = computed(() => {
    const user = this.authState.user();
    if (!user) return undefined;
    return `${user.firstName} ${user.lastName}`;
  });


  constructor() {
    effect(() => {
      const user = this.authState.user();
      if (!user) return;

      this.userType.set(user.userType);
      this.notificationState.init();
    });
  }


  ngOnInit(): void {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.syncRoleWithRoute());

    this.syncRoleWithRoute();
  }


  private syncRoleWithRoute(): void {
    let route = this.router.routerState.root.firstChild;

    while (route?.firstChild) {
      route = route.firstChild;
    }

    const role =
      route?.snapshot?.data?.['dashboardRole'] as ActiveRole | undefined;

    this.activeRole.set(role ?? 'SENDER');
  }


  onRoleChange(role: ActiveRole): void {
    this.router.navigate(['/dashboard', role.toLowerCase()]);
  }

  onMenuAction(action: string): void {
    switch (action) {
      case 'profile':
        this.router.navigateByUrl('/dashboard/profile');
        break;
      case 'settings':
        break;
      case 'logout':
        this.onLogout();
        break;
    }
  }

  onLogout(): void {
    this.authService.logout().subscribe({
      complete: () => this.router.navigateByUrl('/login')
    });
  }

  toggleChat(): void {
    this.chatOpen.update(v => !v);
  }

  closeChat(): void {
    this.chatOpen.set(false);
  }
}
