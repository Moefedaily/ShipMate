import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';


@Component({
  standalone: true,
  selector: 'app-driver-home',
  imports: [CommonModule, MatIconModule],
  templateUrl: './driver-home.page.html',
  styleUrl: './driver-home.page.scss'
})
export class DriverHomePage {}
