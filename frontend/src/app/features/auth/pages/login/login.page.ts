import { Component, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { AuthState } from '../../../../core/auth/auth.state';
import { switchMap, catchError, of } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss'
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly authState = inject(AuthState);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email
    ]),
    password: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(8)
    ])
  });

  readonly emailControl = computed(() => this.form.controls.email);
  readonly passwordControl = computed(() => this.form.controls.password);

  readonly isEmailInvalid = computed(
    () => this.emailControl().touched && this.emailControl().invalid
  );

  readonly isPasswordInvalid = computed(
    () => this.passwordControl().touched && this.passwordControl().invalid
  );

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const { email, password } = this.form.getRawValue();

    this.authService.login(email, password)
      .pipe(
        switchMap(() => this.authService.fetchMe()),
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          this.submitting.set(false);

          if (err.status === 401) {
            this.errorMessage.set('Invalid email or password.');
          } else if (err.status === 403) {
            this.authState.clear();
            this.errorMessage.set(
              'Your email is not verified yet. Please check your inbox.'
            );
          } else {
            this.errorMessage.set(
              err.error?.message || 'Login failed. Please try again.'
            );
          }

          return of(null);
        })
      )
      .subscribe(user => {
        this.submitting.set(false);

        if (!user) return;

        if (user.userType === 'DRIVER') {
          this.router.navigateByUrl('/dashboard/driver');
        } else {
          this.router.navigateByUrl('/dashboard/sender');
        }
      });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }
}