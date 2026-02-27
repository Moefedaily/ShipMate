import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { InsuranceService } from '../../../core/services/insurance/insurance.service';
import { InsuranceClaim } from '../../../core/services/insurance/insurance.model';
import { LoaderService } from '../../../core/ui/loader/loader.service';
import { ToastService } from '../../../core/ui/toast/toast.service';

@Component({
  standalone: true,
  selector: 'app-claims-list-page',
  imports: [CommonModule, MatIconModule],
  templateUrl: './claims-list.page.html',
  styleUrl: './claims-list.page.scss'
})
export class ClaimsListPage implements OnInit {

  private readonly insuranceService = inject(InsuranceService);
  private readonly router = inject(Router);
  private readonly loader = inject(LoaderService);
  private readonly toast = inject(ToastService);

  readonly claims = signal<InsuranceClaim[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.loader.show();

    this.insuranceService.getMyClaims().subscribe({
      next: claims => {
        this.claims.set(claims);
        this.loading.set(false);
        this.loader.hide();
      },
      error: () => {
        this.toast.error('Failed to load claims');
        this.loading.set(false);
        this.loader.hide();
      }
    });
  }

  openClaim(claim: InsuranceClaim): void {
    this.router.navigate([
      '/dashboard/shipments',
      claim.shipmentId,
      'claim',
      'details'
    ]);
  }
  goBack(): void {
    this.router.navigate(['/dashboard/sender']);
    }
}