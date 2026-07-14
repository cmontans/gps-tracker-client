// Speed math — mirrors the Android shared SpeedCalculator / SessionSpeedStats
// and docs/PROTOCOL.md §3. Kept framework-free and unit-testable.

export const GPS = {
  MS_TO_KMH: 3.6,
  KMH_TO_MPH: 0.621371,
  KMH_TO_KNOTS: 0.539957,
  MAX_GPS_ACCURACY: 10.0, // meters
  MIN_SPEED_THRESHOLD: 1.5, // km/h
  MAX_SPEED_READINGS: 20,
  TIME_WINDOW_MS: 10000,
  DISTANCE_WINDOW_M: 500,
  LOCATION_MAX_AGE: 1000
};

// Haversine distance in meters.
export function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function getBearing(lat1, lon1, lat2, lon2) {
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const y = Math.sin(dLon) * Math.cos((lat2 * Math.PI) / 180);
  const x =
    Math.cos((lat1 * Math.PI) / 180) * Math.sin((lat2 * Math.PI) / 180) -
    Math.sin((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.cos(dLon);
  return (((Math.atan2(y, x) * 180) / Math.PI) + 360) % 360;
}

export function convertSpeed(kmh, unit) {
  if (unit === 'mph') return kmh * GPS.KMH_TO_MPH;
  if (unit === 'knots') return kmh * GPS.KMH_TO_KNOTS;
  return kmh;
}

// Derive speed (km/h): hardware speed, else distance/elapsed fallback.
export function deriveSpeedKmh(hasSpeed, speedMs, distanceMeters, elapsedMs) {
  if (hasSpeed) return Math.max(0, speedMs * GPS.MS_TO_KMH);
  if (elapsedMs > 0 && distanceMeters > 0) return Math.max(0, (distanceMeters / (elapsedMs / 1000)) * GPS.MS_TO_KMH);
  return 0;
}

// Rolling avg / 10s / 500m statistics with peaks.
export class SessionSpeedStats {
  constructor() { this.reset(); }
  reset() {
    this.readings = [];
    this.h10 = [];
    this.h500 = [];
    this.total = 0;
    this.avg500 = 0;
    this.max = 0;
    this.max10s = 0;
    this.max500m = 0;
  }
  update(rawSpeed, currentSpeed, timestampMs, distanceMeters) {
    if (rawSpeed > this.max) this.max = rawSpeed;

    this.readings.push(currentSpeed);
    if (this.readings.length > GPS.MAX_SPEED_READINGS) this.readings.shift();
    const avg = this.readings.reduce((a, b) => a + b, 0) / this.readings.length;

    this.h10.push([timestampMs, currentSpeed]);
    while (this.h10.length && timestampMs - this.h10[0][0] > GPS.TIME_WINDOW_MS) this.h10.shift();
    const avg10s = this.h10.reduce((a, b) => a + b[1], 0) / this.h10.length;
    if (avg10s > this.max10s) this.max10s = avg10s;

    this.total += distanceMeters;
    this.h500.push([timestampMs, currentSpeed, this.total]);
    while (this.h500.length > 1 && this.total - this.h500[0][2] > GPS.DISTANCE_WINDOW_M) this.h500.shift();
    if (this.h500.length > 1) {
      const windowDist = this.h500[this.h500.length - 1][2] - this.h500[0][2];
      if (windowDist > 50) {
        this.avg500 = this.h500.reduce((a, b) => a + b[1], 0) / this.h500.length;
        if (this.avg500 > this.max500m) this.max500m = this.avg500;
      }
    }
    return { current: currentSpeed, max: this.max, avg, avg10s, max10s: this.max10s, avg500m: this.avg500, max500m: this.max500m };
  }
}

export function escapeHtml(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
