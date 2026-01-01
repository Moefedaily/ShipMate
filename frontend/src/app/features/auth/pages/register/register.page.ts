import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../../core/auth/auth.service';
import {
  UserType,
  RegisterRequest
} from '../../../../core/auth/auth.models';

@Component({
  standalone: true,
  selector: 'app-register-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss'
})
export class RegisterPage {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  submitting = false;
  success = false;

  readonly form = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('', {
      validators: [Validators.required, Validators.email]
    }),
    firstName: this.fb.nonNullable.control('', Validators.required),
    lastName: this.fb.nonNullable.control('', Validators.required),
    password: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(8)
    ]),
    confirmPassword: this.fb.nonNullable.control('', Validators.required),
    userType: this.fb.nonNullable.control<UserType>('SENDER', Validators.required)
  }, {
    validators: this.passwordMatchValidator
  });

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const {
      email,
      password,
      firstName,
      lastName,
      userType
    } = this.form.getRawValue();

    const request: RegisterRequest = {
      email,
      password,
      firstName,
      lastName,
      userType
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => this.router.navigateByUrl('/login'), 2000);
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  private passwordMatchValidator(group: {
    value: { password: string; confirmPassword: string };
  }) {
    const { password, confirmPassword } = group.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}
