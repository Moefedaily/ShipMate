import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'primary';

@Component({
  selector: 'app-admin-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="'badge--' + variant">{{ label }}</span>`,
  styles: [`
    @use '../../../../../styles/variables/typography' as *;
    @use '../../../admin-colors' as *;

    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      font-size: 11px;
      font-weight: $font-weight-semibold;
      letter-spacing: 0.03em;
      white-space: nowrap;
      text-transform: uppercase;

      &--success { background: $color-success-subtle; color: $color-success; }
      &--warning { background: $color-warning-subtle; color: $color-warning; }
      &--danger  { background: $color-danger-subtle;  color: $color-danger; }
      &--info    { background: $color-info-subtle;    color: $color-info; }
      &--muted   { background: $admin-bg-raised;      color: $admin-text-muted; }
      &--primary { background: $color-primary-subtle; color: $color-primary; }
    }
  `]
})
export class AdminBadgeComponent {
  @Input() label  = '';
  @Input() variant: BadgeVariant = 'muted';
}