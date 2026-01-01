import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
  export class App implements OnInit {

    private readonly authService = inject(AuthService);

    ngOnInit(): void {
      this.authService.restoreSession().subscribe();
    }
  }
