import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-driver-pending-state',
  imports: [CommonModule, MatIconModule],
  templateUrl: './driver-pending.state.html',
  styleUrl: './driver-pending.state.scss'
})
export class DriverPendingState {}
