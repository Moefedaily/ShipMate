import { Directive, ElementRef, EventEmitter, Output, OnDestroy, NgZone } from '@angular/core';

@Directive({
  selector: '[clickOutside]',
  standalone: true
})
export class ClickOutsideDirective implements OnDestroy {
  @Output() clickOutside = new EventEmitter<void>();

  private onDocClick = (event: MouseEvent) => {
    const target = event.target as Node | null;
    if (!target) return;

    if (!this.el.nativeElement.contains(target)) {
      this.clickOutside.emit();
    }
  };

  constructor(private el: ElementRef, private zone: NgZone) {
    this.zone.runOutsideAngular(() => {
      document.addEventListener('click', this.onDocClick, true);
    });
  }

  ngOnDestroy(): void {
    document.removeEventListener('click', this.onDocClick, true);
  }
}
