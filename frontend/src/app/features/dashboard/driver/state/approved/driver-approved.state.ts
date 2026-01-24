import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-driver-approved-state',
  imports: [CommonModule, MatIconModule],
  templateUrl: './driver-approved.state.html',
  styleUrl: './driver-approved.state.scss'
})
export class DriverApprovedState {}
