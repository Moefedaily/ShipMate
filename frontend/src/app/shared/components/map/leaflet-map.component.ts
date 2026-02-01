import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';

import { MapStop, MapStopType } from './leaflet-map.types';

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
export class LeafletMapComponent
  implements AfterViewInit, OnChanges, OnDestroy {

  @ViewChild('mapEl', { static: true })
  private readonly mapEl!: ElementRef<HTMLDivElement>;

  /** PREVIEW = create shipment. ROUTE = driver dashboard. */
  @Input() mode: 'preview' | 'route' = 'preview';

  /** PREVIEW inputs */
  @Input() pickup: MapPoint | null = null;
  @Input() delivery: MapPoint | null = null;

  /** ROUTE inputs */
  @Input() stops: MapStop[] | null = null;

  private map?: L.Map;
  private markers: L.Marker[] = [];
  private routeLine?: L.Polyline;

  private readonly defaultCenter: L.LatLngExpression = [48.8566, 2.3522];
  private readonly defaultZoom = 12;

  ngAfterViewInit(): void {
    this.initMap();
    this.render();
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) return;

    if (
      changes['stops'] ||
      changes['pickup'] ||
      changes['delivery'] ||
      changes['mode']
    ) {
      this.render();
    }
  }


  ngOnDestroy(): void {
    this.clear();
    this.map?.remove();
    this.map = undefined;
  }

  /* ================= MAP INIT ================= */

  private initMap(): void {
    if (this.map) return;

    this.map = L.map(this.mapEl.nativeElement).setView(
      this.defaultCenter,
      this.defaultZoom
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
  }

  /* ================= RENDER ================= */

  private render(): void {
    if (!this.map) return;

    this.clear();

    if (this.mode === 'route') {
      if (this.stops?.length) {
        this.renderActionRoute(this.stops);
      }
      return;
    }

    this.renderSingleRoute();
  }

  /* ================= ROUTE MODE (DRIVER) ================= */

  private renderActionRoute(stops: MapStop[]): void {
    if (!this.map) return;

    const ordered = [...stops].sort((a, b) => a.order - b.order);
    const routePoints: L.LatLngExpression[] = [];

    ordered.forEach(stop => {
      const [lat, lng] = this.offsetLatLng(stop.lat, stop.lng, stop.type);

      const marker = this.createActionMarker(stop, lat, lng);
      marker.addTo(this.map!);
      this.markers.push(marker);

      routePoints.push([lat, lng]);
    });

    if (routePoints.length > 1) {
      this.routeLine = L.polyline(routePoints, {
        color: '#ff7a00',
        weight: 4,
        opacity: 0.60
      }).addTo(this.map);

      this.map.fitBounds(L.latLngBounds(routePoints), { padding: [32, 32] });
    } else if (routePoints.length === 1) {
      this.map.setView(routePoints[0], 14);
    }
  }

  private createActionMarker(stop: MapStop, lat: number, lng: number): L.Marker {
    const icon = this.buildActionIcon(stop.type);

    const marker = L.marker([lat, lng], {
      icon,
      title: stop.address
    });

    marker.bindPopup(this.buildRoutePopup(stop));
    return marker;
  }

  private buildActionIcon(type: MapStopType): L.DivIcon {
    const modifier = type === 'PICKUP' ? 'pickup' : 'delivery';
    const label = type === 'PICKUP' ? 'P' : 'D';

    return L.divIcon({
      className: `map-stop-icon ${modifier}`,
      html: `<div class="stop-badge">${label}</div>`,
      iconSize: [34, 34],
      iconAnchor: [17, 17]
    });
  }

  private offsetLatLng(lat: number, lng: number, type: MapStopType): [number, number] {
    const delta = 0.000035; // ~3–4 meters
    return type === 'PICKUP' ? [lat + delta, lng] : [lat - delta, lng];
  }

  private buildRoutePopup(stop: MapStop): string {
    const title = stop.type === 'PICKUP' ? 'Pickup' : 'Delivery';

    const shipments = stop.shipments
      .map(s => `• ${s.label} (${s.weightKg} kg)`)
      .join('<br/>');

    return `
      <strong>${title}</strong><br/>
      ${stop.address}<br/><br/>
      ${shipments}
    `;
  }

  /* ================= PREVIEW MODE (CREATE) ================= */

  private renderSingleRoute(): void {
    if (!this.map || !this.pickup || !this.delivery) return;

    const pickupLatLng: L.LatLngExpression = [
      this.pickup.lat,
      this.pickup.lng
    ];

    const deliveryLatLng: L.LatLngExpression = [
      this.delivery.lat,
      this.delivery.lng
    ];

    // Pickup marker
    const pickupMarker = L.marker(pickupLatLng, {
      icon: L.divIcon({
        className: 'preview-marker pickup',
        html: '',
        iconSize: [14, 14],
        iconAnchor: [7, 7]
      }),
      title: this.pickup.label
    });

    // Delivery marker
    const deliveryMarker = L.marker(deliveryLatLng, {
      icon: L.divIcon({
        className: 'preview-marker delivery',
        html: '',
        iconSize: [14, 14],
        iconAnchor: [7, 7]
      }),
      title: this.delivery.label
    });

    pickupMarker.addTo(this.map);
    deliveryMarker.addTo(this.map);

    this.markers.push(pickupMarker, deliveryMarker);

    this.routeLine = L.polyline(
      [pickupLatLng, deliveryLatLng],
      {
        color: '#ff7a00',
        weight: 3,
        opacity: 0.6
      }
    ).addTo(this.map);

    this.map.fitBounds(
      L.latLngBounds([pickupLatLng, deliveryLatLng]),
      { padding: [32, 32] }
    );
  }


  private buildPreviewDot(kind: 'pickup' | 'delivery'): L.DivIcon {
    return L.divIcon({
      className: `preview-marker ${kind}`,
      html: '',
      iconSize: [14, 14],
      iconAnchor: [7, 7]
    });
  }


  private clear(): void {
    this.markers.forEach(m => this.map?.removeLayer(m));
    this.markers = [];

    if (this.routeLine) {
      this.map?.removeLayer(this.routeLine);
      this.routeLine = undefined;
    }
  }
}
