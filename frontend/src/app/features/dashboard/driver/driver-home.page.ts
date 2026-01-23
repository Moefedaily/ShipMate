import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { DriverDashboardState } from './state/driver-dashboard.state';

import { DriverApprovedState } from './state/approved/driver-approved.state';
import { DriverNotAppliedState } from './state/not-applied/driver-not-applied.state';
import { DriverPendingState } from './state/pending/driver-pending.state';
import { DriverRejectedState } from './components/driver-rejected-state/driver-rejected-state.component';
import { DriverSuspendedState } from './components/driver-suspended-state/driver-suspended-state.component';
import { DriverApplyFormComponent } from './components/driver-apply-form/driver-apply-form.component';

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
    DriverApplyFormComponent
  ],
  templateUrl: './driver-home.page.html',
  styleUrl: './driver-home.page.scss'
})
export class DriverHomePage {

  private readonly route = inject(ActivatedRoute);

  readonly DriverDashboardState = DriverDashboardState;

  // Resolver guarantees this is ready before render
  readonly state = signal<DriverDashboardState>(
    this.route.snapshot.data['state']
  );

  // ---------- UI events ----------

  onApply(): void {
    this.state.set(DriverDashboardState.APPLYING);
  }

  onApplied(): void {
    this.state.set(DriverDashboardState.PENDING);
  }
}
