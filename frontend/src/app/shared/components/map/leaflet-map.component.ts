import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';

import * as L from 'leaflet';

export interface MapPoint {
  lat: number;
  lng: number;
  label?: string;
}

@Component({
  selector: 'app-leaflet-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './leaflet-map.component.html',
  styleUrl: './leaflet-map.component.scss'
})
export class LeafletMapComponent implements AfterViewInit, OnChanges {

  @ViewChild('mapEl', { static: true }) private readonly mapEl!: ElementRef<HTMLDivElement>;

  @Input() pickup: MapPoint | null = null;
  @Input() delivery: MapPoint | null = null;

  private map?: L.Map;
  private pickupMarker?: L.Marker;
  private deliveryMarker?: L.Marker;
  private routeLine?: L.Polyline;

  private readonly defaultCenter: L.LatLngExpression = [48.8566, 2.3522]; // Paris fallback
  private readonly defaultZoom = 12;

  ngAfterViewInit(): void {
    this.initMap();
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) return;
    if (changes['pickup'] || changes['delivery']) {
      this.render();
    }
  }

  private initMap(): void {
    if (this.map) return;

    this.map = L.map(this.mapEl.nativeElement, {
      zoomControl: true,
      attributionControl: true
    }).setView(this.defaultCenter, this.defaultZoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    // Fix broken default marker icons in bundlers
    this.fixDefaultMarkerIcons();
  }

  private render(): void {
    if (!this.map) return;

    // Clear polyline first
    if (this.routeLine) {
      this.map.removeLayer(this.routeLine);
      this.routeLine = undefined;
    }

    // Pickup marker
    if (this.pickup) {
      if (!this.pickupMarker) {
        this.pickupMarker = L.marker([this.pickup.lat, this.pickup.lng],{ title: 'Pickup location' }).addTo(this.map);
      } else {
        this.pickupMarker.setLatLng([this.pickup.lat, this.pickup.lng]);
      }

      if (this.pickup.label) {
        this.pickupMarker.bindPopup(this.pickup.label);
      }
    } else if (this.pickupMarker) {
      this.map.removeLayer(this.pickupMarker);
      this.pickupMarker = undefined;
    }

    // Delivery marker
    if (this.delivery) {
      if (!this.deliveryMarker) {
        this.deliveryMarker = L.marker([this.delivery.lat, this.delivery.lng],{ title: 'Delivery location' }).addTo(this.map);
      } else {
        this.deliveryMarker.setLatLng([this.delivery.lat, this.delivery.lng]);
      }

      if (this.delivery.label) {
        this.deliveryMarker.bindPopup(this.delivery.label);
      }
    } else if (this.deliveryMarker) {
      this.map.removeLayer(this.deliveryMarker);
      this.deliveryMarker = undefined;
    }

    // Route polyline + fit bounds
    if (this.pickup && this.delivery) {
      const points: L.LatLngExpression[] = [
        [this.pickup.lat, this.pickup.lng],
        [this.delivery.lat, this.delivery.lng]
      ];

      this.routeLine = L.polyline(points, { weight: 4, opacity: 0.9, color: '#ff7a00' }).addTo(this.map);

      const bounds = L.latLngBounds(points);
      this.map.fitBounds(bounds, { padding: [28, 28] });
      return;
    }

    // If only one point -> center on it nicely
    if (this.pickup && !this.delivery) {
      this.map.setView([this.pickup.lat, this.pickup.lng], 14);
      return;
    }

    if (this.delivery && !this.pickup) {
      this.map.setView([this.delivery.lat, this.delivery.lng], 14);
      return;
    }

    // None -> fallback
    this.map.setView(this.defaultCenter, this.defaultZoom);
  }

  private fixDefaultMarkerIcons(): void {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const DefaultIcon = (L.Icon.Default as any);

    delete DefaultIcon.prototype._getIconUrl;

    L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      iconUrl: 'assets/leaflet/marker-icon.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png'
    });
  }
}
