import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoaderService } from './loader.service';

@Component({
  selector: 'app-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loader.isLoading()) {
      <div class="loader-backdrop">
        <div class="spinner"></div>
      </div>
    }
  `,
  styleUrl: './loader.component.scss'
})
export class LoaderComponent {
  readonly loader = inject(LoaderService);
}
