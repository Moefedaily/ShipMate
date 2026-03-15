import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

import {
  DriverProfileResponse,
  DriverStatus,
  UpdateLicenseRequest,
  VehicleType,
} from '../../../../core/services/driver/driver.models';
import { DriverService } from '../../../../core/services/driver/driver.service';
import {
  CreateVehicleRequest,
  Vehicle,
} from '../../../../core/services/driver/vehicle/vehicle.models';
import { VehicleService } from '../../../../core/services/driver/vehicle/vehicle.service';
import { LoaderService } from '../../../../core/ui/loader/loader.service';
import { ToastService } from '../../../../core/ui/toast/toast.service';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-driver-vehicles-page',
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './driver-vehicles.page.html',
  styleUrl: './driver-vehicles.page.scss'
})
export class DriverVehiclesPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly driverService = inject(DriverService);
  private readonly vehicleService = inject(VehicleService);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly driverProfile = signal<DriverProfileResponse | null>(null);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly submitting = signal(false);
  readonly showVehicleForm = signal(false);
  readonly showLicenseForm = signal(false);
  readonly licensePhotos = signal<File[]>([]);
  readonly licensePhotoPreviews = computed(() =>
    this.licensePhotos().map(file => URL.createObjectURL(file))
  );

  readonly activeVehicle = computed(() =>
    this.vehicles().find(v => v.active) ?? null
  );

  readonly driverStatus = computed(() =>
    this.driverProfile()?.status ?? null
  );

  readonly canAddVehicle = computed(() =>
    this.driverStatus() === DriverStatus.APPROVED
  );

  readonly vehicleAreaLocked = computed(() =>
    this.driverStatus() !== DriverStatus.APPROVED
  );

  readonly vehicleTypes: VehicleType[] = [
    VehicleType.BICYCLE,
    VehicleType.MOTORCYCLE,
    VehicleType.CAR,
    VehicleType.VAN,
    VehicleType.TRUCK
  ];

  readonly vehicleIcons: Record<VehicleType, string> = {
    BICYCLE: 'pedal_bike',
    MOTORCYCLE: 'two_wheeler',
    CAR: 'directions_car',
    VAN: 'airport_shuttle',
    TRUCK: 'local_shipping'
  };

  readonly vehicleLabels: Record<VehicleType, string> = {
    BICYCLE: 'Bicycle',
    MOTORCYCLE: 'Motorcycle',
    CAR: 'Car',
    VAN: 'Van',
    TRUCK: 'Truck'
  };

  readonly vehicleForm = this.fb.nonNullable.group({
    vehicleType: [VehicleType.CAR, Validators.required],
    maxWeightCapacity: [null as unknown as number, [Validators.required, Validators.min(1)]],
    plateNumber: [''],
    insuranceExpiry: [''],
    vehicleDescription: ['']
  });

  readonly licenseForm = this.fb.nonNullable.group({
    licenseNumber: ['', Validators.required],
    licenseExpiry: ['', Validators.required]
  });

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loader.show();

    this.driverService.getMyDriverProfile().subscribe({
      next: profile => {
        this.driverProfile.set(profile);
        this.licenseForm.patchValue({
          licenseNumber: profile.licenseNumber ?? '',
          licenseExpiry: profile.licenseExpiry ?? ''
        });
      },
      error: () => {}
    });

    this.vehicleService.getMyVehicles().subscribe({
      next: vehicles => {
        this.vehicles.set(vehicles);
        this.loader.hide();
      },
      error: () => this.loader.hide()
    });
  }

  onLicenseFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.licensePhotos.set(Array.from(input.files));
      input.value = '';
    }
  }

  removeLicensePhoto(index: number): void {
    this.licensePhotos.update(list => list.filter((_, i) => i !== index));
  }

  submitLicense(): void {
    if (this.licenseForm.invalid || this.submitting() || this.licensePhotos().length === 0) {
      this.licenseForm.markAllAsTouched();
      this.toast.error('License details and at least one photo are required.');
      return;
    }

    this.submitting.set(true);
    const request: UpdateLicenseRequest = this.licenseForm.getRawValue();

    this.driverService.updateLicense(request).subscribe({
      next: () => {
        this.driverService.uploadLicensePhotos(this.licensePhotos()).subscribe({
          next: () => {
            const currentProfile = this.driverProfile();
            if (currentProfile) {
              this.driverProfile.set({
                ...currentProfile,
                status: DriverStatus.PENDING,
                approvedAt: null
              });
            }
            this.showLicenseForm.set(false);
            this.showVehicleForm.set(false);
            this.licensePhotos.set([]);
            this.submitting.set(false);
            this.vehicles.update(list => list.map(vehicle => ({ ...vehicle, active: false })));
            this.toast.success('License resubmitted. Vehicle actions are locked until admin approval.');
          },
          error: err => {
            this.submitting.set(false);
            this.toast.error(err?.error?.message || 'License updated, but photo upload failed.');
          }
        });
      },
      error: err => {
        this.submitting.set(false);
        this.toast.error(err?.error?.message || 'Failed to update license.');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard/profile']);
  }
  submitVehicle(): void {
    if (this.vehicleForm.invalid || this.submitting()) {
      this.vehicleForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.vehicleForm.getRawValue();
    const req: CreateVehicleRequest = {
      vehicleType: raw.vehicleType,
      maxWeightCapacity: raw.maxWeightCapacity,
      plateNumber: raw.plateNumber || undefined,
      insuranceExpiry: raw.insuranceExpiry || undefined,
      vehicleDescription: raw.vehicleDescription || undefined
    };

    this.vehicleService.addVehicle(req).subscribe({
      next: vehicle => {
        this.vehicles.set([vehicle, ...this.vehicles()]);
        this.showVehicleForm.set(false);
        this.vehicleForm.reset({
          vehicleType: VehicleType.CAR,
          maxWeightCapacity: null as unknown as number,
          plateNumber: '',
          insuranceExpiry: '',
          vehicleDescription: ''
        });
        this.submitting.set(false);
        this.toast.success('Vehicle added successfully.');
      },
      error: err => {
        this.submitting.set(false);
        this.toast.error(err?.error?.message || 'Failed to add vehicle.');
      }
    });
  }

  activate(vehicle: Vehicle): void {
    if (vehicle.active || vehicle.status !== 'APPROVED') return;

    this.loader.show();
    this.vehicleService.activateVehicle(vehicle.id).subscribe({
      next: updated => {
        this.vehicles.set(
          this.vehicles().map(v =>
            v.id === updated.id ? updated : { ...v, active: false }
          )
        );
        this.loader.hide();
        this.toast.success('Vehicle activated.');
      },
      error: err => {
        this.loader.hide();
        this.toast.error(err?.error?.message || 'Failed to activate vehicle.');
      }
    });
  }

  trackById = (_: number, vehicle: Vehicle) => vehicle.id;
}
