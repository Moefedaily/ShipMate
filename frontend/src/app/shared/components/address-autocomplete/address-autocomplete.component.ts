import {
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
  OnDestroy,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  Subject,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  of,
  takeUntil
} from 'rxjs';

export interface AddressResult {
  address: string;
  lat: number;
  lng: number;
}

@Component({
  selector: 'app-address-autocomplete',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './address-autocomplete.component.html',
  styleUrl: './address-autocomplete.component.scss'
})
export class AddressAutocompleteComponent
  implements OnDestroy, OnChanges {

  /* ---------- Inputs ---------- */

  @Input() label = '';
  @Input() placeholder = 'Enter address';
  @Input() disabled = false;

  @Input() initialValue: string | null = null;

  /* ---------- Outputs ---------- */

  @Output() selectAddress = new EventEmitter<AddressResult>();

  /* ---------- State ---------- */

  readonly query = signal('');
  readonly results = signal<AddressResult[]>([]);
  readonly loading = signal(false);

  /* ---------- Internals ---------- */

  private readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly http: HttpClient) {
    this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          if (query.length < 3) {
            this.loading.set(false);
            return of([]);
          }

          this.loading.set(true);

          return this.http.get<any[]>(
            'https://nominatim.openstreetmap.org/search',
            {
              headers: {
                Accept: 'application/json',
                'User-Agent': 'ShipMate/1.0 (contact@shipmate.app)'
              },
              params: {
                q: query,
                format: 'json',
                addressdetails: '1',
                limit: '5'
              }
            }
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: res => {
          const mapped = res.map(r => ({
            address: r.display_name,
            lat: Number(r.lat),
            lng: Number(r.lon)
          }));

          this.results.set(mapped);
          this.loading.set(false);
        },
        error: () => {
          this.results.set([]);
          this.loading.set(false);
        }
      });
  }

  /* ---------- Handle initial value ---------- */

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialValue'] && this.initialValue) {
      this.query.set(this.initialValue);
    }
  }

  /* ---------- Events ---------- */

  handleInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.onInput(value);
  }

  onInput(value: string): void {
    this.query.set(value);
    this.search$.next(value);
  }

  select(result: AddressResult): void {
    this.query.set(result.address);
    this.results.set([]);
    this.selectAddress.emit(result);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
