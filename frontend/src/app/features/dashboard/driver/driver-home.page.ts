import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { DriverDashboardState } from './state/driver-dashboard.state';

import { DriverApprovedState } from './state/approved/driver-approved.state';
import { DriverNotAppliedState } from './state/not-applied/driver-not-applied.state';
import { DriverPendingState } from './state/pending/driver-pending.state';
import { DriverRejectedState } from './components/driver-rejected-state/driver-rejected-state.component';
import { DriverSuspendedState } from './components/driver-suspended-state/driver-suspended-state.component';
import { DriverApplyFormComponent } from './components/driver-apply-form/driver-apply-form.component';
import { DriverLocationSetupPage } from './setup/driver-location-setup.page';

@Component({
  standalone: true,
  selector: 'app-driver-home',
  imports: [
    CommonModule,
    DriverApprovedState,
    DriverNotAppliedState,
    DriverPendingState,
    DriverRejectedState,
    DriverSuspendedState,
    DriverApplyFormComponent,
    DriverLocationSetupPage,
  ],
  templateUrl: './driver-home.page.html',
  styleUrl: './driver-home.page.scss'
})
export class DriverHomePage {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly DriverDashboardState = DriverDashboardState;

  readonly state = signal<DriverDashboardState>(DriverDashboardState.INIT);

  constructor() {
    this.route.data.subscribe(data => {
      this.state.set(data['state']);
    });
  }

  // ---------- UI events ----------

  onApply(): void {
    this.state.set(DriverDashboardState.APPLYING);
  }
  onLocationCompleted(): void {
    this.state.set(DriverDashboardState.APPROVED);
  }

  onApplied(): void {
    this.state.set(DriverDashboardState.PENDING);
  }
}
