import { inject, Injectable, signal } from "@angular/core";
import { DriverEarningResponse, DriverEarningsSummaryResponse } from "../../services/earning/earning.model";
import { EarningService } from "../../services/earning/earning.service";

@Injectable()
export class EarningsState {

  private readonly earningService = inject(EarningService);

  readonly summary = signal<DriverEarningsSummaryResponse | null>(null);
  readonly earnings = signal<DriverEarningResponse[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.earningService.getSummary().subscribe({
      next: summary => this.summary.set(summary),
      error: () => this.errorMessage.set('Failed to load summary')
    });

    this.earningService.getEarnings().subscribe({
      next: page => {
        this.earnings.set(page.content ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load earnings');
        this.loading.set(false);
      }
    });
  }
}
