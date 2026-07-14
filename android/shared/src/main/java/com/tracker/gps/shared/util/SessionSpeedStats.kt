package com.tracker.gps.shared.util

/**
 * Pure, framework-free rolling speed statistics for a single tracking session.
 *
 * Extracted from LocationTrackingService so the 10s / 500m windowing math is
 * unit-testable and shared between the phone and (potentially) the watch. All
 * speeds are in km/h. Call [update] once per accepted GPS fix, passing the
 * distance in meters travelled since the previous fix.
 *
 * Behavior mirrors the web client:
 *  - avg   : mean of the last [maxSpeedReadings] thresholded speeds
 *  - 10s   : mean/peak of thresholded speed over a trailing [timeWindowMs] window
 *  - 500m  : mean/peak of thresholded speed over a trailing [distanceWindowM] window
 */
class SessionSpeedStats(
    private val maxSpeedReadings: Int = Constants.MAX_SPEED_READINGS,
    private val timeWindowMs: Long = Constants.AVG_SPEED_TIME_WINDOW,
    private val distanceWindowM: Double = Constants.AVG_SPEED_DISTANCE_WINDOW
) {
    data class Snapshot(
        val current: Double,
        val max: Double,
        val avg: Double,
        val avg10s: Double,
        val max10s: Double,
        val avg500m: Double,
        val max500m: Double
    )

    private val speedReadings = mutableListOf<Double>()
    private val history10s = mutableListOf<Pair<Long, Double>>() // timestamp, speed
    private val history500m = mutableListOf<Triple<Long, Double, Double>>() // timestamp, speed, cumulativeDistance

    private var totalDistance = 0.0
    private var avg500m = 0.0 // persists between fixes until the window has enough data

    var max = 0.0
        private set
    var max10s = 0.0
        private set
    var max500m = 0.0
        private set

    /**
     * @param rawSpeed raw GPS speed before the stationary threshold (used for peak max)
     * @param currentSpeed speed after the stationary threshold (used for averages)
     * @param timestampMs timestamp of this fix, in ms
     * @param distanceMeters distance travelled since the previous fix (0 for the first fix)
     */
    fun update(rawSpeed: Double, currentSpeed: Double, timestampMs: Long, distanceMeters: Double): Snapshot {
        if (rawSpeed > max) max = rawSpeed

        // Rolling average of the last N readings
        speedReadings.add(currentSpeed)
        if (speedReadings.size > maxSpeedReadings) speedReadings.removeAt(0)
        val avg = if (speedReadings.isNotEmpty()) speedReadings.average() else 0.0

        // 10s trailing window
        history10s.add(Pair(timestampMs, currentSpeed))
        while (history10s.isNotEmpty() && timestampMs - history10s[0].first > timeWindowMs) {
            history10s.removeAt(0)
        }
        val avg10s = if (history10s.isNotEmpty()) history10s.map { it.second }.average() else 0.0
        if (avg10s > max10s) max10s = avg10s

        // 500m trailing window
        totalDistance += distanceMeters
        history500m.add(Triple(timestampMs, currentSpeed, totalDistance))
        while (history500m.size > 1 && totalDistance - history500m[0].third > distanceWindowM) {
            history500m.removeAt(0)
        }
        if (history500m.size > 1) {
            val windowDist = history500m.last().third - history500m.first().third
            if (windowDist > 50.0) { // only report once we have at least 50m of data
                avg500m = history500m.map { it.second }.average()
                if (avg500m > max500m) max500m = avg500m
            }
        }

        return Snapshot(currentSpeed, max, avg, avg10s, max10s, avg500m, max500m)
    }

    fun reset() {
        speedReadings.clear()
        history10s.clear()
        history500m.clear()
        totalDistance = 0.0
        avg500m = 0.0
        max = 0.0
        max10s = 0.0
        max500m = 0.0
    }
}
