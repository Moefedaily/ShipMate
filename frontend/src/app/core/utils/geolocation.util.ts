export interface GeoPosition {
  lat: number;
  lng: number;
}

export function requestBrowserLocation(
  timeoutMs = 10_000
): Promise<GeoPosition> {
  return new Promise((resolve, reject) => {

    if (!navigator.geolocation) {
      reject(new Error('Geolocation not supported'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      pos => {
        resolve({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude
        });
      },
      err => {
        reject(err);
      },
      {
        enableHighAccuracy: true,
        timeout: timeoutMs
      }
    );
  });
}
