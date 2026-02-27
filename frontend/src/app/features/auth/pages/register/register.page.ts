import { Component, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { UserType, RegisterRequest } from '../../../../core/auth/auth.models';

@Component({
  standalone: true,
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss'
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // UI state
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  // Form
  readonly form = this.fb.nonNullable.group(
    {
      email: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.email
      ]),
      firstName: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.minLength(2)
      ]),
      lastName: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.minLength(2)
      ]),
      password: this.fb.nonNullable.control('', [
        Validators.required,
        this.passwordStrengthValidator
      ]),
      confirmPassword: this.fb.nonNullable.control('', Validators.required),
      userType: this.fb.nonNullable.control<UserType>('SENDER', Validators.required)
    },
    { validators: this.passwordMatchValidator }
  );

  // Computed helpers
  readonly emailControl = computed(() => this.form.controls.email);
  readonly passwordControl = computed(() => this.form.controls.password);
  readonly confirmPasswordControl = computed(() => this.form.controls.confirmPassword);
  readonly firstNameControl = computed(() => this.form.controls.firstName);
  readonly lastNameControl = computed(() => this.form.controls.lastName);

  readonly isEmailInvalid = computed(
    () => this.emailControl().touched && this.emailControl().invalid
  );
  readonly isPasswordInvalid = computed(
    () => this.passwordControl().touched && this.passwordControl().invalid
  );
  readonly isFirstNameInvalid = computed(
    () => this.firstNameControl().touched && this.firstNameControl().invalid
  );
  readonly isLastNameInvalid = computed(
    () => this.lastNameControl().touched && this.lastNameControl().invalid
  );
  readonly hasPasswordMismatch = computed(
    () => this.confirmPasswordControl().touched && this.form.errors?.['passwordMismatch']
  );

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const { confirmPassword, ...payload } = this.form.getRawValue();
    const request: RegisterRequest = payload;

    this.authService.register(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.router.navigateByUrl('/check-email');
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(
            err.error?.message ||
            'Registration failed. Please try again.'
          );
        }
      });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update(v => !v);
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value as string;
    if (!value) return null;

    const hasNumber = /[0-9]/.test(value);
    const hasUpper = /[A-Z]/.test(value);
    const hasLower = /[a-z]/.test(value);

    return hasNumber && hasUpper && hasLower ? null : { weakPassword: true };
  }
}