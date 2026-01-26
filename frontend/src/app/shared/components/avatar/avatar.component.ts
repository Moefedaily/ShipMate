import { Component, EventEmitter, Input, Output } from '@angular/core';

type AvatarSize = 'xs' | 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-avatar',
  standalone: true,
  templateUrl: './avatar.component.html',
  styleUrl: './avatar.component.scss'
})
export class AvatarComponent {

  @Input() src?: string | null;
  @Input() size: AvatarSize = 'md';
  @Input() editable = false;

  @Output() upload = new EventEmitter<File>();
  @Output() remove = new EventEmitter<void>();

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    input.value = '';
    this.upload.emit(file);
  }
}
