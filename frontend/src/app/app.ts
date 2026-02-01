import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './core/ui/toast/toast.component';
import { LoaderComponent } from './core/ui/loader/loader.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet,LoaderComponent,ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
