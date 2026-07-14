package com.tracker.gps.shared.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun uses_hardware_speed_when_available() {
        // 10 m/s -> 36 km/h
        assertEquals(36.0, SpeedCalculator.deriveSpeedKmh(true, 10.0f, 0.0, 0L), 0.0001)
    }

    @Test
    fun clamps_negative_hardware_speed_to_zero() {
        assertEquals(0.0, SpeedCalculator.deriveSpeedKmh(true, -5.0f, 0.0, 0L), 0.0001)
    }

    @Test
    fun falls_back_to_distance_over_time_when_no_hardware_speed() {
        // 100 m in 10 s = 10 m/s = 36 km/h
        assertEquals(36.0, SpeedCalculator.deriveSpeedKmh(false, 0.0f, 100.0, 10_000L), 0.0001)
    }

    @Test
    fun returns_zero_when_no_speed_and_no_movement() {
        assertEquals(0.0, SpeedCalculator.deriveSpeedKmh(false, 0.0f, 0.0, 1_000L), 0.0001)
        // No elapsed time (first fix) -> zero, avoids division by zero
        assertEquals(0.0, SpeedCalculator.deriveSpeedKmh(false, 0.0f, 50.0, 0L), 0.0001)
    }
}
