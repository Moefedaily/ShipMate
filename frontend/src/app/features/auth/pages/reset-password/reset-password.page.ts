import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-reset-password-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reset-password.page.html',
  styleUrl: './reset-password.page.scss'
})
export class ResetPasswordPage {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  submitting = false;
  success = false;
  token: string | null;

  readonly form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: this.passwordMatchValidator }
  );

  constructor() {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      // Invalid access → send back to login
      this.router.navigateByUrl('/login');
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting || !this.token) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const { password } = this.form.getRawValue();

    this.authService.resetPassword(this.token, password).subscribe({
      next: () => {
        this.success = true;
        this.submitting = false;

        // Redirect after short delay
        setTimeout(() => {
          this.router.navigateByUrl('/login');
        }, 2000);
      },
      error: () => {
        // Token invalid / expired
        this.submitting = false;
        this.form.setErrors({ invalidToken: true });
      }
    });
  }

  private passwordMatchValidator(group: any) {
    const { password, confirmPassword } = group.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}
