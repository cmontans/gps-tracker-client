package com.tracker.gps.shared.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.max

class AltitudeKalmanFilterTest {

    private lateinit var filter: AltitudeKalmanFilter

    @Before
    fun setup() {
        filter = AltitudeKalmanFilter()
        filter.reset(0.0)
    }

    @Test
    fun test_smooths_surface_chop() {
        // Simulate: A baseline altitude of 0.0m with high-frequency noise oscillating between -0.5m and +0.5m
        val noisePattern = listOf(0.5, -0.2, 0.4, -0.5, 0.1, -0.4, 0.3, -0.3, 0.2, -0.1, 0.5, -0.5)
        
        var maxDeviation = 0.0
        
        for (i in 1..100) {
            val noise = noisePattern[i % noisePattern.size]
            val filtered = filter.update(noise)
            
            // Allow the filter to settle for the first few iterations
            if (i > 10) {
                if (Math.abs(filtered) > maxDeviation) {
                    maxDeviation = Math.abs(filtered)
                }
            }
        }
        
        // Assert: The filtered output remains relatively stable around 0.0m (within ±0.15m)
        assertTrue("Filter failed to smooth chop. Max deviation: $maxDeviation", maxDeviation <= 0.15)
    }

    @Test
    fun test_ignores_wetsuit_squeeze() {
        // Simulate: A stable baseline of 0.0m for 20 readings
        for (i in 1..20) {
            filter.update(0.0)
        }
        
        // Massive drop to -8.0m for 4 readings (the squeeze)
        var lowestPoint = 0.0
        for (i in 1..4) {
            val filtered = filter.update(-8.0)
            if (filtered < lowestPoint) {
                lowestPoint = filtered
            }
        }
        
        // Immediately return to 0.0m
        for (i in 1..10) {
            filter.update(0.0)
        }
        
        // Assert: The filtered output should not drop below -1.5m before recovering
        assertTrue("Filter dropped too far during squeeze. Lowest point: $lowestPoint", lowestPoint >= -1.5)
    }

    @Test
    fun test_tracks_true_jump() {
        // Simulate: A smooth, parabolic progression from 0.0m up to 12.0m over 2 seconds (50 readings)
        val numReadings = 50
        var maxFilteredAltitude = 0.0
        
        val noisePattern = listOf(0.2, -0.1, -0.2, 0.1, 0.0, 0.2, -0.2)
        
        for (i in 0..numReadings) {
            // Parabola: y = a * x * (reading_count - x)
            // Apex at x = 25 where y = 12.0. So 12.0 = a * 25 * 25 => a = 12.0 / 625 = 0.0192
            val a = 12.0 / (25 * 25)
            val trueAltitude = a * i * (numReadings - i)
            
            val noise = noisePattern[i % noisePattern.size]
            val reading = trueAltitude + noise
            
            val filtered = filter.update(reading)
            if (filtered > maxFilteredAltitude) {
                maxFilteredAltitude = filtered
            }
        }
        
        // Assert: The maximum filtered altitude recorded is within ±0.5m of the true 12.0m apex
        assertEquals(12.0, maxFilteredAltitude, 0.5)
    }
}
