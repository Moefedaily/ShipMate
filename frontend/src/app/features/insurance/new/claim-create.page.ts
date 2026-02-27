import { Component, inject, signal, computed, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

import { ShipmentService } from '../../../core/services/shipment/shipment.service';
import { LoaderService } from '../../../core/ui/loader/loader.service';
import { ToastService } from '../../../core/ui/toast/toast.service';

import { ShipmentResponse } from '../../../core/services/shipment/shipment.models';
import { ClaimReason } from '../../../core/services/insurance/insurance.model';
import { InsuranceState } from '../../../core/state/insurance/InsuranceState';

@Component({
  standalone: true,
  selector: 'app-claim-create-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule
  ],
  templateUrl: './claim-create.page.html',
  styleUrl: './claim-create.page.scss'
})
export class ClaimCreatePage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shipmentService = inject(ShipmentService);
  private readonly insuranceState = inject(InsuranceState);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly shipment = signal<ShipmentResponse | null>(null);
  readonly loading = signal(true);
  readonly photos = signal<File[]>([]);

  readonly form = this.fb.nonNullable.group({
    claimReason: ['LOST' as ClaimReason, Validators.required],
    description: ['']
  });

  readonly isDamaged = computed(() =>
    this.form.controls.claimReason.value === 'DAMAGED'
  );

  readonly availableReasons = computed<ClaimReason[]>(() => {
    const s = this.shipment();
    if (!s) return [];
    if (s.status === 'IN_TRANSIT' || s.status === 'LOST') return ['LOST'];
    if (s.status === 'DELIVERED') return ['DAMAGED', 'OTHER'];
    return [];
  });

  readonly statusValidForReason = computed(() => {
    const current = this.form.controls.claimReason.value;
    return this.availableReasons().includes(current);
  });

  readonly canSubmit = computed(() => {
    if (!this.form.valid) return false;
    if (!this.statusValidForReason()) return false;
    if (this.isDamaged() && this.photos().length === 0) return false;
    return true;
  });

  readonly isInsured = computed(() => {
    const s = this.shipment();
    return !!s?.insuranceSelected && !!s?.insuranceFee && s.insuranceFee > 0;
  });

  constructor() {
    effect(() => {
      const allowed = this.availableReasons();
      const current = this.form.controls.claimReason.value;
      if (allowed.length === 0) return;
      if (!allowed.includes(current)) {
        this.form.controls.claimReason.setValue(allowed[0]);
      }
    });
  }

  ngOnInit(): void {
    const shipmentId = this.route.snapshot.paramMap.get('id');
    if (!shipmentId) {
      this.router.navigate(['/dashboard/shipments']);
      return;
    }

    this.loader.show();

    this.shipmentService.getMyShipment(shipmentId).subscribe({
      next: shipment => {
        this.shipment.set(shipment);
        this.loading.set(false);
        this.loader.hide();
      },
      error: () => {
        this.toast.error('Unable to load shipment');
        this.router.navigate(['/dashboard/shipments']);
        this.loader.hide();
      }
    });
  }


  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    this.photos.update(list => [...list, ...Array.from(input.files!)]);
    input.value = '';
  }

  removePhoto(index: number): void {
    const previews = this.photoPreviews();
    URL.revokeObjectURL(previews[index].url);

    this.photos.update(list => list.filter((_, i) => i !== index));
    }
  readonly photoPreviews = computed(() =>
    this.photos().map(file => ({
        file,
        url: URL.createObjectURL(file)
    }))
    );
  submit(): void {
    if (!this.canSubmit()) return;
    const shipment = this.shipment();
    if (!shipment) return;

    this.loader.show();

    const request = {
      claimReason: this.form.controls.claimReason.value,
      description: this.form.controls.description.value || undefined
    };

    this.insuranceState['insuranceService']
      .submitClaim(shipment.id, request)
      .subscribe({
        next: (claim) => {
          if (this.photos().length === 0) {
            this.finishSuccess(shipment.id);
            return;
          }
          this.insuranceState['insuranceService']
            .addClaimPhotos(claim.id, this.photos())
            .subscribe({
              next: () => this.finishSuccess(shipment.id),
              error: () => {
                this.loader.hide();
                this.toast.error('Claim created but photo upload failed');
              }
            });
        },
        error: (err) => {
          this.loader.hide();
          this.toast.error(err.error?.message || 'Failed to submit claim');
        }
      });
  }

  private finishSuccess(shipmentId: string): void {
    this.loader.hide();
    this.toast.success('Claim submitted successfully');
    this.router.navigate(['/dashboard/shipments', shipmentId, 'claim', 'details']);
  }

  goBack(): void {
    this.router.navigate(['/dashboard/shipments']);
  }
}