import { CommonModule } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { RouterLink } from "@angular/router";
import { EarningsState } from "../../core/state/earnings/earnings.state";

@Component({
  standalone: true,
  selector: 'app-earnings-page',
  imports: [CommonModule, MatIconModule],
  providers: [EarningsState],
  templateUrl: './earnings.page.html',
  styleUrl: './earnings.page.scss'
})
export class EarningsPage implements OnInit {

  private readonly state = inject(EarningsState);

  readonly summary = this.state.summary;
  readonly earnings = this.state.earnings;
  readonly loading = this.state.loading;
  readonly errorMessage = this.state.errorMessage;

  ngOnInit(): void {
    this.state.load();
  }
}
