import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, of } from 'rxjs';

import { DriverDashboardState } from './state/driver-dashboard.state';

import { DriverApprovedState } from './state/approved/driver-approved.state';
import { DriverNotAppliedState } from './state/not-applied/driver-not-applied.state';
import { DriverPendingState } from './state/pending/driver-pending.state';
import { DriverRejectedState } from './components/driver-rejected-state/driver-rejected-state.component';
import { DriverSuspendedState } from './components/driver-suspended-state/driver-suspended-state.component';
import { DriverApplyFormComponent } from './components/driver-apply-form/driver-apply-form.component';

import { DriverService } from '../../../core/driver/driver.service';
import { mapDriverStatusToDashboardState } from '../../../core/driver/driver.mapper';

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
export class DriverHomePage implements OnInit {

  private readonly driverService = inject(DriverService);

  readonly DriverDashboardState = DriverDashboardState;

  // Controls first render
  readonly loading = signal(true);

  // Single source of truth for dashboard UI
  readonly state = signal<DriverDashboardState>(
    DriverDashboardState.INIT
  );

  ngOnInit(): void {
    this.loadDriverState();
  }

  private loadDriverState(): void {
    this.driverService.getMyDriverProfile()
      .pipe(
        catchError(err => {

          // 404 → user never applied
          if (err.status === 404) {
            this.state.set(DriverDashboardState.NOT_APPLIED);
            this.loading.set(false);
            return of(null);
          }

          // fallback safety
          this.state.set(DriverDashboardState.NOT_APPLIED);
          this.loading.set(false);
          return of(null);
        })
      )
      .subscribe(profile => {
        if (profile) {
          const mappedState =
            mapDriverStatusToDashboardState(profile.status);

          this.state.set(mappedState);
        }

        this.loading.set(false);
      });
  }

  // ---------- UI events ----------

  onApply(): void {
    this.state.set(DriverDashboardState.APPLYING);
  }

  onApplied(): void {
    this.state.set(DriverDashboardState.PENDING);
  }
}
