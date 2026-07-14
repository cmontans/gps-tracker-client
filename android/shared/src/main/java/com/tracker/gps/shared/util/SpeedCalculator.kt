package com.tracker.gps.shared.util

/**
 * Derives instantaneous speed (km/h) from a GPS fix.
 *
 * Prefers the hardware-reported speed, but falls back to distance / elapsed-time
 * when the platform does not supply a speed on the fix — matching the web client,
 * which computed a haversine fallback while the Android clients previously
 * reported 0 in that case.
 */
object SpeedCalculator {
    /**
     * @param hasSpeed whether the fix reported a hardware speed
     * @param speedMs hardware speed in m/s (used only when [hasSpeed] is true)
     * @param distanceMeters distance from the previous fix, in meters
     * @param elapsedMs time since the previous fix, in ms
     */
    fun deriveSpeedKmh(hasSpeed: Boolean, speedMs: Float, distanceMeters: Double, elapsedMs: Long): Double {
        if (hasSpeed) {
            return (speedMs * Constants.MS_TO_KMH).coerceAtLeast(0.0)
        }
        if (elapsedMs > 0 && distanceMeters > 0) {
            val fallbackMs = distanceMeters / (elapsedMs / 1000.0)
            return (fallbackMs * Constants.MS_TO_KMH).coerceAtLeast(0.0)
        }
        return 0.0
    }
}
