import { HttpClient } from "@angular/common/http";
import { DriverEarningsSummaryResponse } from "./earning.model";
import { environment } from "../../../../environments/environment";
import { inject, Injectable } from "@angular/core";

@Injectable({ providedIn: 'root' })
export class EarningService {

  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBaseUrl;

  getSummary() {
    return this.http.get<DriverEarningsSummaryResponse>(
      `${this.api}/earnings/summary`
    );
  }

  getEarnings(page = 0) {
    return this.http.get<any>(
      `${this.api}/earnings?page=${page}`
    );
  }
}
