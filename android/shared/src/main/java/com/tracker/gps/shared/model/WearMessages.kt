package com.tracker.gps.shared.model

/**
 * Messages for communication between phone and smartwatch via Wearable Data Layer
 */
object WearPaths {
    const val TRACKING_STATE = "/tracking/state"
    const val LOCATION_UPDATE = "/tracking/location"
    const val USERS_UPDATE = "/tracking/users"
    const val CONTROL_COMMAND = "/tracking/control"
    const val CONNECTION_STATUS = "/tracking/connection"
    const val STATS_UPDATE = "/tracking/stats"
    const val GROUP_HORN = "/tracking/horn"
}

/**
 * Control commands sent from watch to phone or vice versa
 */
sealed class WearControlCommand {
    data class StartTracking(
        val userName: String,
        val groupName: String
    ) : WearControlCommand()

    object StopTracking : WearControlCommand()
    object TriggerGroupHorn : WearControlCommand()
    object ResetStats : WearControlCommand()
}

/**
 * Tracking state shared between phone and watch
 */
data class TrackingState(
    val isTracking: Boolean,
    val userName: String = "",
    val groupName: String = "",
    val currentSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Connection status information
 */
data class ConnectionStatus(
    val isConnected: Boolean,
    val hasGps: Boolean,
    val serverUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Statistics summary
 */
data class TrackingStats(
    val currentSpeed: Double,
    val maxSpeed: Double,
    val avgSpeed: Double,
    val distance: Double = 0.0,
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
