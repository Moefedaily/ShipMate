import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-forgot-password-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './forgot-password.page.html',
  styleUrl: './forgot-password.page.scss'
})
export class ForgotPasswordPage {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  submitting = false;
  submitted = false;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const { email } = this.form.getRawValue();

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.submitted = true;
        this.submitting = false;
      },
      error: () => {
        // SECURITY: still show success
        this.submitted = true;
        this.submitting = false;
      }
    });
  }
}
