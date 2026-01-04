import { Component, inject, signal, computed, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.page.html',
  styleUrl: './reset-password.page.scss'
})
export class ResetPasswordPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // Signals for reactive state
  readonly submitting = signal(false);
  readonly success = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly token = signal<string | null>(null);

  // Form definition
  readonly form = this.fb.nonNullable.group(
    {
      password: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.minLength(8),
        this.passwordStrengthValidator
      ]),
      confirmPassword: this.fb.nonNullable.control('', Validators.required)
    },
    { validators: this.passwordMatchValidator }
  );

  // Computed getters for cleaner template access
  readonly passwordControl = computed(() => this.form.controls.password);
  readonly confirmPasswordControl = computed(() => this.form.controls.confirmPassword);

  readonly isPasswordInvalid = computed(() => 
    this.passwordControl().touched && this.passwordControl().invalid
  );
  readonly hasPasswordMismatch = computed(() => 
    this.confirmPasswordControl().touched && this.form.errors?.['passwordMismatch']
  );

  ngOnInit(): void {
    const tokenParam = this.route.snapshot.queryParamMap.get('token');
    
    if (!tokenParam) {
      this.router.navigateByUrl('/login');
      return;
    }

    this.token.set(tokenParam);
  }

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.submitting() || !this.token()) return;

    this.submitting.set(true);

    const { password } = this.form.getRawValue();

    this.authService.resetPassword(this.token()!, password)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.success.set(true);
          this.submitting.set(false);
          this.form.disable();
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(
            err.error?.message || 
            'This reset link is invalid or has expired. Please request a new one.'
          );
        }
      });
  }

  navigateToLogin(): void {
    this.router.navigateByUrl('/login');
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(val => !val);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update(val => !val);
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

    const valid = hasNumber && hasUpper && hasLower;
    return valid ? null : { weakPassword: true };
  }
}