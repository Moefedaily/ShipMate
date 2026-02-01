import { Component, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { catchError, of } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.page.html',
  styleUrl: './forgot-password.page.scss'
})
export class ForgotPasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // Signals for reactive state
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Form definition
  readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email
    ])
  });

  // Computed getters for cleaner template access
  readonly emailControl = computed(() => this.form.controls.email);
  readonly isEmailInvalid = computed(() => 
    this.emailControl().touched && this.emailControl().invalid
  );

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.submitting()) return;

    this.submitting.set(true);

    const { email } = this.form.getRawValue();

    this.authService.forgotPassword(email)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          // SECURITY: Don't reveal if email exists
          // Still show success message regardless of error
          console.error('Password reset error:', err);
          return of(null);
        })
      )
      .subscribe({
        next: () => {
          this.submitted.set(true);
          this.submitting.set(false);
          this.form.disable();
        }
      });
  }

  resetForm(): void {
    this.submitted.set(false);
    this.errorMessage.set(null);
    this.form.reset();
    this.form.enable();
  }
}