import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-driver-not-applied-state',
  imports: [CommonModule, MatIconModule],
  templateUrl: './driver-not-applied.state.html',
  styleUrl: './driver-not-applied.state.scss'
})
export class DriverNotAppliedState {

  @Output()
  applyClick = new EventEmitter<void>();

  onApply(): void {
    this.applyClick.emit();
  }
}
