package com.tracker.gps.util

/**
 * 1D Kalman Filter to smooth raw barometer readings.
 * Crucial to mitigate "wetsuit squeeze" (rapid false pressure changes).
 */
class AltitudeKalmanFilter(
    private val processNoise: Double = 0.1,    // Q: Expected rate of altitude change
    private val measurementNoise: Double = 1.0 // R: Base noise dampening
) {
    private var altitude: Double = 0.0
    private var variance: Double = 1.0
    private var isInitialized: Boolean = false

    /**
     * Resets the filter with an initial altitude reading.
     */
    fun reset(initialAltitude: Double) {
        altitude = initialAltitude
        variance = 1.0
        isInitialized = true
    }

    /**
     * Updates the filter with a new measurement and returns the smoothed altitude.
     */
    fun update(measurement: Double): Double {
        if (!isInitialized) {
            reset(measurement)
            return measurement
        }

        // Prediction Phase
        val predictedVariance = variance + processNoise

        // Adaptive Measurement Noise (Robust Kalman Filter)
        // If the residual is huge (e.g. > 3.0 meters instantly), it's likely a false reading
        // like a wetsuit squeeze impact. We temporarily spike the measurement noise.
        val residual = Math.abs(measurement - altitude)
        val adaptiveMeasurementNoise = if (residual > 3.0) measurementNoise * 20 else measurementNoise

        // Update Phase
        val kalmanGain = predictedVariance / (predictedVariance + adaptiveMeasurementNoise)
        altitude += kalmanGain * (measurement - altitude)
        variance = (1 - kalmanGain) * predictedVariance

        return altitude
    }

    fun getCurrentAltitude(): Double = altitude
}
