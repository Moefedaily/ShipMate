import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-driver-rejected-state',
    standalone: true,
    imports: [CommonModule, MatIconModule],
    templateUrl: './driver-rejected-state.component.html',
    styleUrls: ['./driver-rejected-state.component.scss'],
})
export class DriverRejectedState {}