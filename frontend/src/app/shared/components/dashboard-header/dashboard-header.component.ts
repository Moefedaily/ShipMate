import { Component, computed, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

import { AuthState } from '../../../core/auth/auth.state';
import { AvatarComponent } from '../avatar/avatar.component';
import { NotificationsComponent } from '../notifications/notifications.component';

export type UserType = 'SENDER' | 'DRIVER' | 'BOTH';
export type ActiveRole = 'SENDER' | 'DRIVER';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
    MatDividerModule,
    AvatarComponent,
    NotificationsComponent
  ],
  templateUrl: './dashboard-header.component.html',
  styleUrl: './dashboard-header.component.scss'
})
export class DashboardHeaderComponent {

  private readonly authState = inject(AuthState);

  readonly user = computed(() => this.authState.user());

  readonly userName = computed(() => {
    const u = this.user();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  @Input() userType?: UserType;
  @Input() activeRole?: ActiveRole;

  @Output() roleChange = new EventEmitter<ActiveRole>();
  @Output() menuAction = new EventEmitter<string>();
  @Output() chatToggle = new EventEmitter<void>();

  canToggleRole(): boolean {
    return this.userType === 'BOTH';
  }

  isSender(): boolean {
    return this.activeRole === 'SENDER';
  }

  isDriver(): boolean {
    return this.activeRole === 'DRIVER';
  }

  switchRole(role: ActiveRole): void {
    if (this.activeRole !== role) {
      this.roleChange.emit(role);
    }
  }

  showDriverWallet(): boolean {
  return this.activeRole === 'DRIVER';
}
  onMenu(action: string): void {
    this.menuAction.emit(action);
  }
}
