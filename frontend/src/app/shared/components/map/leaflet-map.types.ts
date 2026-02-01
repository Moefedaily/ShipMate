export type MapStopType = 'PICKUP' | 'DELIVERY';

export interface MapStop {
  id: string;
  type: MapStopType;
  order: number;
  lat: number;
  lng: number;
  address: string;
  shipments: MapStopShipment[];
}

export interface MapStopShipment {
  id: string;
  label: string;
  weightKg: number;
  pickupAddress: string;
  deliveryAddress: string;
}
