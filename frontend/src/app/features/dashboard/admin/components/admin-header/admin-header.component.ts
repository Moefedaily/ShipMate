import { Component, computed, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Router } from '@angular/router';
import { AuthState } from '../../../../../core/auth/auth.state';
import { AuthService } from '../../../../../core/auth/auth.service';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './admin-header.component.html',
  styleUrl: './admin-header.component.scss',
})
export class AdminHeaderComponent {
  /** Pass the current page title from the layout */
  @Input() pageTitle = 'Dashboard';

  /** Sidebar expanded state — used to offset the header correctly */
  @Input() sidebarExpanded = false;

  private readonly authState   = inject(AuthState);
  private readonly authService = inject(AuthService);
  private readonly router      = inject(Router);

  readonly user     = computed(() => this.authState.user());
  readonly userName = computed(() => {
    const u = this.user();
    return u ? `${u.firstName} ${u.lastName}` : 'Admin';
  });
  readonly userInitial = computed(() => this.userName().charAt(0).toUpperCase());

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: () => this.router.navigateByUrl('/login')
    });
  }
  goToApp(): void {
    this.router.navigateByUrl('/dashboard');
  }
}