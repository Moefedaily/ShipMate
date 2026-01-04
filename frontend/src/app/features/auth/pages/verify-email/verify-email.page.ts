import { Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-verify-email-page',
  imports: [RouterLink],
  templateUrl: './verify-email.page.html',
  styleUrl: './verify-email.page.scss'
})
export class VerifyEmailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // Signals for reactive state
  readonly loading = signal(true);
  readonly success = signal(false);
  readonly error = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Computed for template clarity
  readonly isVerifying = computed(() => this.loading());
  readonly isSuccess = computed(() => this.success());
  readonly isError = computed(() => this.error());

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.loading.set(false);
      this.error.set(true);
      this.errorMessage.set('Missing verification token.');
      return;
    }

    this.authService.verifyEmail(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(true);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(true);
          this.errorMessage.set(
            err.error?.message || 
            'This verification link is invalid or has expired.'
          );
        }
      });
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}