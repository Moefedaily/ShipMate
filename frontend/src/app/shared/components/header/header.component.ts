import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

export type UserType = 'SENDER' | 'DRIVER' | 'BOTH';
export type ActiveRole = 'SENDER' | 'DRIVER';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  /* ---------- Inputs ---------- */
  @Input() isAuthenticated = false;
  @Input() userName?: string;
  @Input() userAvatarUrl?: string;
  @Input() userType?: UserType;
  @Input() activeRole?: ActiveRole;
  @Input() unreadNotificationsCount = 0;
  @Input() unreadMessagesCount = 0;

  /* ---------- Outputs ---------- */
  @Output() roleChange = new EventEmitter<ActiveRole>();
  @Output() notificationsClick = new EventEmitter<void>();
  @Output() messagesClick = new EventEmitter<void>();
  @Output() profileClick = new EventEmitter<void>();
  @Output() settingsClick = new EventEmitter<void>();
  @Output() logoutClick = new EventEmitter<void>();
  @Output() loginClick = new EventEmitter<void>();
  @Output() signupClick = new EventEmitter<void>();

  /* ---------- Helpers ---------- */
  canSwitchRole(): boolean {
    return this.isAuthenticated && this.userType === 'BOTH';
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
}