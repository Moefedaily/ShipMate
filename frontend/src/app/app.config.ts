import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/http/jwt.interceptor';
import { AuthService } from './core/auth/auth.service';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

function initializeIcons() {
  return () => {
    const iconRegistry = inject(MatIconRegistry);
    const sanitizer = inject(DomSanitizer);

    ['facebook', 'twitter', 'instagram'].forEach(icon => {
      iconRegistry.addSvgIcon(
        icon,
        sanitizer.bypassSecurityTrustResourceUrl(`assets/icons/${icon}.svg`)
      );
    });
  };
}

function initializeAuth() {
  return () => {
    const authService = inject(AuthService);
    return firstValueFrom(
      authService.refreshAccessToken()
    ).catch(() => null);
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),

    provideAppInitializer(initializeIcons()),
    provideAppInitializer(initializeAuth())
  ]
};
