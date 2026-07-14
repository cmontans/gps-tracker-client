package com.tracker.gps.shared.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSpeedStatsTest {

    @Test
    fun max_uses_raw_speed_before_threshold() {
        val stats = SessionSpeedStats()
        // rawSpeed above, currentSpeed thresholded to 0 (stationary noise)
        val snap = stats.update(rawSpeed = 5.0, currentSpeed = 0.0, timestampMs = 0L, distanceMeters = 0.0)
        assertEquals(5.0, snap.max, 0.0001)
        assertEquals(0.0, snap.current, 0.0001)
    }

    @Test
    fun rolling_average_is_mean_of_recent_readings() {
        val stats = SessionSpeedStats()
        stats.update(10.0, 10.0, 0L, 0.0)
        val snap = stats.update(20.0, 20.0, 1000L, 10.0)
        assertEquals(15.0, snap.avg, 0.0001)
    }

    @Test
    fun five_hundred_meter_average_is_computed_once_window_has_data() {
        // Regression test: the 500m average was permanently 0 because distance was
        // measured from a point to itself. Feed constant 36 km/h with 10m deltas.
        val stats = SessionSpeedStats()
        var snap: SessionSpeedStats.Snapshot? = null
        for (i in 0..7) {
            val distance = if (i == 0) 0.0 else 10.0
            snap = stats.update(36.0, 36.0, i * 1000L, distance)
        }
        // After ~70m of travel the trailing window exceeds the 50m minimum,
        // so avg/max 500m should reflect the constant 36 km/h — not 0.
        assertTrue("avg500m should be non-zero once window has data", snap!!.avg500m > 0.0)
        assertEquals(36.0, snap.avg500m, 0.5)
        assertEquals(36.0, snap.max500m, 0.5)
    }

    @Test
    fun ten_second_window_drops_old_readings() {
        val stats = SessionSpeedStats()
        // Old reading at t=0
        stats.update(10.0, 10.0, 0L, 0.0)
        // New reading 11s later — the old one is outside the 10s window
        val snap = stats.update(30.0, 30.0, 11000L, 10.0)
        assertEquals("only the recent reading should remain in the 10s window", 30.0, snap.avg10s, 0.0001)
    }

    @Test
    fun reset_clears_all_accumulated_state() {
        val stats = SessionSpeedStats()
        for (i in 0..7) {
            stats.update(36.0, 36.0, i * 1000L, if (i == 0) 0.0 else 10.0)
        }
        stats.reset()
        val snap = stats.update(0.0, 0.0, 0L, 0.0)
        assertEquals(0.0, snap.max, 0.0001)
        assertEquals(0.0, snap.max10s, 0.0001)
        assertEquals(0.0, snap.max500m, 0.0001)
        assertEquals(0.0, snap.avg500m, 0.0001)
    }
}
