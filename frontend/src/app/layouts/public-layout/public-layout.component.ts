import { Component, inject, computed } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { AuthService } from '../../core/auth/auth.service';
import { AuthState } from '../../core/auth/auth.state';

type UserType = 'SENDER' | 'DRIVER' | 'BOTH';
type ActiveRole = 'SENDER' | 'DRIVER';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    FooterComponent
  ],
  templateUrl: './public-layout.component.html',
  styleUrls: ['./public-layout.component.scss']
})
export class PublicLayoutComponent {

  private readonly authState = inject(AuthState);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /* ---------- Derived UI State (READ ONLY) ---------- */

  readonly isAuthenticated = this.authState.isAuthenticated;

  readonly userName = computed(() =>
    this.authState.user()?.email
  );

  readonly userType = computed<UserType | undefined>(() =>
    this.authState.user()?.userType
  );

  readonly activeRole = computed<ActiveRole | undefined>(() => {
    const type = this.authState.user()?.userType;
    return type === 'BOTH' ? 'SENDER' : type;
  });

  /* ---------- Header Events ---------- */

  onRoleChange(role: ActiveRole): void {
    // future enhancement
  }

  onLogout(): void {
    this.authService.logout().subscribe();
  }

  onLogin(): void {
    this.router.navigateByUrl('/login');
  }

  onSignup(): void {
    this.router.navigateByUrl('/register');
  }
}
