package com.tracker.gps.shared.util

object SpeedFormatter {
    fun formatSpeed(speed: Double): String {
        return String.format("%.1f", speed)
    }

    fun formatSpeedWithUnit(speed: Double): String {
        return String.format("%.1f km/h", speed)
    }

    fun formatCoordinate(coordinate: Double): String {
        return String.format("%.6f", coordinate)
    }
}
