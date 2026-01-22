import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';



@Component({
  standalone: true,
  selector: 'app-sender-home',
  imports: [CommonModule, MatIconModule],
  templateUrl: './sender-home.page.html',
  styleUrl: './sender-home.page.scss'
})
export class SenderHomePage {}
