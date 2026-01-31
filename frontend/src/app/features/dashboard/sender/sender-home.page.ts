import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-sender-home',
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './sender-home.page.html',
  styleUrl: './sender-home.page.scss'
})
export class SenderHomePage {}
