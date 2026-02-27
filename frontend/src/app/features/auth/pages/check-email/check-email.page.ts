import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-check-email-page',
  templateUrl: './check-email.page.html',
  styleUrl: './check-email.page.scss'
})
export class CheckEmailPage {
  private readonly router = inject(Router);

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}
