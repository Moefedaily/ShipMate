import { Component, inject, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DriverService } from '../../../../../core/driver/driver.service';
import {
  DriverApplyRequest,
  VEHICLE_TYPE_LABELS,
  VehicleType
} from '../../../../../core/driver/driver.models';

@Component({
  standalone: true,
  selector: 'app-driver-apply-form',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './driver-apply-form.component.html',
  styleUrl: './driver-apply-form.component.scss'
})
export class DriverApplyFormComponent {

  private readonly fb = inject(FormBuilder);
  private readonly driverService = inject(DriverService);

  @Output() applied = new EventEmitter<void>();

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    licenseNumber: ['', Validators.required],
    vehicleType: ['', Validators.required],
    maxWeightCapacity: [0, [Validators.required, Validators.min(1)]],
    vehicleDescription: ['', Validators.required],
  });

  readonly vehicleTypes = Object.values(VehicleType);
  readonly vehicleTypeLabels = VEHICLE_TYPE_LABELS;

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const request: DriverApplyRequest = this.form.getRawValue();

    this.driverService.applyAsDriver(request).subscribe({
      next: () => {
        this.form.disable();
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
