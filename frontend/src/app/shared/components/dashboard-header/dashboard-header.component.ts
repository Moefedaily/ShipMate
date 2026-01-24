import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

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
    MatDividerModule
  ],
  templateUrl: './dashboard-header.component.html',
  styleUrl: './dashboard-header.component.scss'
})
export class DashboardHeaderComponent {

  /* ---------- Inputs ---------- */
  @Input() userName?: string;
  @Input() userAvatarUrl?: string;
  @Input() userType?: UserType;
  @Input() activeRole?: ActiveRole;

  /* ---------- Outputs ---------- */
  @Output() roleChange = new EventEmitter<ActiveRole>();
  @Output() menuAction = new EventEmitter<string>();

  /* ---------- Helpers ---------- */

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

  onMenu(action: string): void {
    this.menuAction.emit(action);
  }
}
