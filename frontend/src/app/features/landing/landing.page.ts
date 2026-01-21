import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

type HowTab = 'SENDER' | 'DRIVER';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './landing.page.html',
  styleUrls: ['./landing.page.scss']
})
export class LandingPage {
  activeHowTab: HowTab = 'SENDER';

  setHowTab(tab: HowTab): void {
    this.activeHowTab = tab;
  }

  isSenderTab(): boolean {
    return this.activeHowTab === 'SENDER';
  }

  isDriverTab(): boolean {
    return this.activeHowTab === 'DRIVER';
  }
}
