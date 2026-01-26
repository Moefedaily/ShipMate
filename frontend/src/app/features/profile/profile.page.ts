import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { catchError, of } from 'rxjs';

import { AuthState } from '../../core/auth/auth.state';
import { AuthService } from '../../core/auth/auth.service';

import { DriverService } from '../../core/driver/driver.service';
import {
  DriverProfileResponse,
  VEHICLE_TYPE_LABELS
} from '../../core/driver/driver.models';

import { UserService } from '../../core/user/user.service';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';
import { LoaderService } from '../../core/ui/loader/loader.service';
import { ToastService } from '../../core/ui/toast/toast.service';


@Component({
  standalone: true,
  selector: 'app-profile-page',
  imports: [CommonModule, ReactiveFormsModule,AvatarComponent],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss'
})
export class ProfilePage implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly authState = inject(AuthState);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly driverService = inject(DriverService);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly avatarUrl = computed(() => this.user()?.avatarUrl ?? null);

  /* ---------------- UI State ---------------- */

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /* ---------------- Driver Profile ---------------- */

  readonly driverProfile = signal<DriverProfileResponse | null>(null);

  /* ---------------- Form ---------------- */

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    phone: ['']
  });

  /* ---------------- Snapshot ---------------- */
  readonly formSnapshot = signal<{ firstName: string; lastName: string; phone: string } | null>(null);
 
  /* ---------------- Lifecycle ---------------- */

    ngOnInit(): void {
    this.initUserForm();
    this.loadDriverProfile();

    this.form.valueChanges.subscribe(value => {
        if (!value) return;

        this.formSnapshot.set({
        firstName: value.firstName ?? '',
        lastName: value.lastName ?? '',
        phone: value.phone ?? ''
        });
    });
    }

  private initUserForm(): void {
    const user = this.authState.user();
    if (!user) return;

    this.form.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? ''
    });
  }

  private loadDriverProfile(): void {
    this.driverService.getMyDriverProfile()
      .pipe(
        catchError(err => {
          if (err.status === 404) {
            this.driverProfile.set(null);
            return of(null);
          }
          return of(null);
        })
      )
      .subscribe(profile => {
        if (profile) {
          this.driverProfile.set(profile);
        }
      });
  }

  /* ---------------- Computed (User) ---------------- */

  readonly user = computed(() => this.authState.user());

  readonly email = computed(() => this.user()?.email);
  readonly verified = computed(() => !!this.user()?.verified);
  readonly active = computed(() => !!this.user()?.active);
  readonly userType = computed(() => this.user()?.userType);

  /* ---------------- Validation ---------------- */

  readonly isFirstNameInvalid = computed(() =>
    this.form.controls.firstName.touched &&
    this.form.controls.firstName.invalid
  );

  readonly isLastNameInvalid = computed(() =>
    this.form.controls.lastName.touched &&
    this.form.controls.lastName.invalid
  );

  readonly hasChanges = computed(() => {
    const user = this.user();
    const snapshot = this.formSnapshot();

    if (!user || !snapshot) return false;

    return (
        snapshot.firstName !== user.firstName ||
        snapshot.lastName !== user.lastName ||
        snapshot.phone !== user.phone
    );
    });

  /* ---------------- Driver Computed ---------------- */

  readonly showDriverSection = computed(() => !!this.driverProfile());

  readonly driverStatus = computed(() =>
    this.driverProfile()?.status
  );

  readonly driverApprovedAt = computed(() =>
    this.driverProfile()?.approvedAt
  );

  readonly driverMaxWeight = computed(() =>
    this.driverProfile()?.maxWeightCapacity
  );

  readonly driverVehicleTypeLabel = computed(() => {
    const type = this.driverProfile()?.vehicleType;
    return type ? VEHICLE_TYPE_LABELS[type] : '';
  });

  /* ---------------- Actions ---------------- */

  save(): void {
    if (this.form.invalid || !this.hasChanges()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loader.show();
    this.errorMessage.set(null);

    this.userService.updateMyProfile(this.form.getRawValue()).subscribe({
      next: updatedUser => {
        this.authService.updateCachedUser(updatedUser);
        this.toast.success('Profile updated successfully');
        this.form.markAsPristine();
        this.form.markAsUntouched();
        this.loader.hide();
      },
      error: err => {
        this.toast.error(
          err.error?.message || 'Unable to update profile'
        );
        this.loader.hide();
      }
    });
  }

  onAvatarUpload(file: File): void {
    this.loader.show();
    this.errorMessage.set(null);

    this.userService.uploadAvatar(file).subscribe({
      next: user => {
        this.authService.updateCachedUser(user);
        this.toast.success('Profile photo updated');
        this.loader.hide();
      },
      error: err => {
        this.toast.error(
          err.error?.message || 'Failed to upload profile photo'
        );
        this.loader.hide();
      }
    });
  }


  removeAvatar(): void {
    const confirmed = confirm(
      'Remove your profile photo? This action cannot be undone.'
    );

    if (!confirmed) return;

    this.loader.show();

    this.userService.deleteAvatar().subscribe({
      next: () => {
        this.authService.updateCachedUser({
          ...this.user()!,
          avatarUrl: null
        });
        this.toast.success('Profile photo removed');
        this.loader.hide();
      },
      error: err => {
        this.toast.error(
          err.error?.message || 'Failed to remove profile photo'
        );
        this.loader.hide();
      }
    });
  }

}
