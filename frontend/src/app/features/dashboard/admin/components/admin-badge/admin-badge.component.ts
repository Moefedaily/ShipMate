import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'primary';

@Component({
  selector: 'app-admin-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="'badge--' + variant">{{ label }}</span>`,
  styleUrl: './admin-badge.component.scss',
})
export class AdminBadgeComponent {
  @Input() label   = '';
  @Input() variant: BadgeVariant = 'muted';
}