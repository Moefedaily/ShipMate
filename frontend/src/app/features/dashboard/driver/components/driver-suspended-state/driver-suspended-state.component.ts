import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-driver-suspended-state',
    standalone: true,
    imports: [CommonModule, MatIconModule],
    templateUrl: './driver-suspended-state.component.html',
    styleUrls: ['./driver-suspended-state.component.scss'],
})
export class DriverSuspendedState {}