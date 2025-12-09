package com.tracker.gps.shared

object Constants {
    // WebSocket
    const val DEFAULT_SERVER_URL = "wss://gps-tracker-server-production-5900.up.railway.app"

    // Location Settings
    const val LOCATION_UPDATE_INTERVAL_MS = 1000L
    const val LOCATION_FASTEST_INTERVAL_MS = 500L

    // Speed Calculation
    const val MS_TO_KMH = 3.6
    const val AVERAGE_WINDOW_SIZE = 20

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "gps_tracker_channel"
    const val NOTIFICATION_CHANNEL_NAME = "GPS Tracking"
    const val NOTIFICATION_ID = 1

    // Wearable Data Layer Paths
    const val PATH_TRACKING_STATE = "/tracking_state"
    const val PATH_SPEED_UPDATE = "/speed_update"
    const val PATH_USERS_UPDATE = "/users_update"
    const val PATH_START_TRACKING = "/start_tracking"
    const val PATH_STOP_TRACKING = "/stop_tracking"
    const val PATH_RESET_STATS = "/reset_stats"
    const val PATH_GROUP_HORN = "/group_horn"
    const val PATH_CONNECTION_STATUS = "/connection_status"

    // Data Keys
    const val KEY_CURRENT_SPEED = "current_speed"
    const val KEY_MAX_SPEED = "max_speed"
    const val KEY_AVG_SPEED = "avg_speed"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_BEARING = "bearing"
    const val KEY_CONNECTED = "connected"
    const val KEY_GPS_ACTIVE = "gps_active"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_GROUP_NAME = "group_name"
    const val KEY_USERS_JSON = "users_json"
    const val KEY_TRACKING_ACTIVE = "tracking_active"
}
