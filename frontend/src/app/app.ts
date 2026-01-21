import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { AuthService } from './core/auth/auth.service';
import { AuthState } from './core/auth/auth.state';
import { AuthUser } from './core/auth/auth.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';


type UserType = 'SENDER' | 'DRIVER' | 'BOTH';
type ActiveRole = 'SENDER' | 'DRIVER';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly authState = inject(AuthState);
  private readonly iconRegistry = inject(MatIconRegistry);
  private readonly sanitizer = inject(DomSanitizer);


  /* ---------- UI State for Header ---------- */
  isAuthenticated = false;
  userName?: string;
  userType?: UserType;
  activeRole?: ActiveRole;

  ngOnInit(): void {
    this.registerIcons();
    this.authService.restoreSession().subscribe(user => {
      this.applyUser(user);
    });
  }

  private applyUser(user: AuthUser | null): void {
    if (!user) {
      this.isAuthenticated = false;
      this.userName = undefined;
      this.userType = undefined;
      this.activeRole = undefined;
      return;
    }

    this.isAuthenticated = true;
    this.userName = user.email; // or full name??????
    this.userType = user.userType;

    // Default active role
    if (user.userType === 'BOTH') {
      this.activeRole = 'SENDER';
    } else {
      this.activeRole = user.userType;
    }
  }

  /* ---------- Header Events ---------- */

  onRoleChange(role: ActiveRole) {
    this.activeRole = role;
    // later: persist this in AuthState or backend!!!!!
  }

  onLogout() {
    this.authService.logout().subscribe(() => {
      this.applyUser(null);
    });
  }

  onLogin() {
    // router.navigate(['/auth/login'])
  }

  onSignup() {
    // router.navigate(['/auth/register'])
  }
  private registerIcons(): void {
  this.iconRegistry.addSvgIcon(
    'twitter',
    this.sanitizer.bypassSecurityTrustResourceUrl(
      'twitter.svg'
    )
  );

  this.iconRegistry.addSvgIcon(
    'instagram',
    this.sanitizer.bypassSecurityTrustResourceUrl(
      'instagram.svg'
    )
  );

  this.iconRegistry.addSvgIcon(
    'facebook',
    this.sanitizer.bypassSecurityTrustResourceUrl(
      'facebook.svg'
    )
  );
}

}
