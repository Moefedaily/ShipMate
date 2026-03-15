import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

import { DriverApplyRequest, VEHICLE_TYPE_LABELS, VehicleType } from '../../../../../core/services/driver/driver.models';
import { DriverService } from '../../../../../core/services/driver/driver.service';

@Component({
  standalone: true,
  selector: 'app-driver-apply-form',
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './driver-apply-form.component.html',
  styleUrl: './driver-apply-form.component.scss'
})
export class DriverApplyFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly driverService = inject(DriverService);

  @Output() applied = new EventEmitter<void>();

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly photos = signal<File[]>([]);
  readonly photoPreviews = computed(() =>
    this.photos().map(file => URL.createObjectURL(file))
  );

  readonly form = this.fb.nonNullable.group({
    licenseNumber: ['', Validators.required],
    licenseExpiry: ['', Validators.required],
    vehicleType: [VehicleType.CAR, Validators.required],
    maxWeightCapacity: [null as unknown as number, [Validators.required, Validators.min(0.1)]],
    plateNumber: [''],
    vehicleDescription: [''],
  });

  readonly vehicleTypes = Object.values(VehicleType);
  readonly vehicleTypeLabels = VEHICLE_TYPE_LABELS;

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.photos.set(Array.from(input.files));
      input.value = '';
    }
  }

  removePhoto(index: number): void {
    this.photos.update(list => list.filter((_, i) => i !== index));
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || this.photos().length === 0) {
      this.form.markAllAsTouched();
      this.errorMessage.set(this.photos().length === 0 ? 'At least one license photo is required.' : null);
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const request: DriverApplyRequest = {
      ...raw,
      plateNumber: raw.plateNumber || undefined,
      vehicleDescription: raw.vehicleDescription || undefined
    };

    this.driverService.applyAsDriverWithPhotos(request, this.photos()).subscribe({
      next: () => {
        this.form.disable();
        this.submitting.set(false);
        this.applied.emit();
      },
      error: err => {
        this.submitting.set(false);
        this.errorMessage.set(
          err.error?.message || 'Unable to submit driver application.'
        );
      }
    });
  }
}
