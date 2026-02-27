import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './core/ui/toast/toast.component';
import { LoaderComponent } from './core/ui/loader/loader.component';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet,LoaderComponent,ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {  
    private readonly authService = inject(AuthService);
  constructor() {
    this.authService.initWsEffect();
  }}
