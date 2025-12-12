package com.tracker.gps.shared.util

object Constants {
    // Wearable Data Layer
    const val CAPABILITY_TRACKER_APP = "gps_tracker_app"

    // Shared Preferences
    const val PREF_NAME = "gps_tracker_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_GROUP_NAME = "group_name"
    const val PREF_SERVER_URL = "server_url"
    const val PREF_AUTO_CENTER = "auto_center"
    const val PREF_VOICE_ENABLED = "voice_enabled"
    const val PREF_VOICE_MIN_SPEED = "voice_min_speed"
    const val PREF_VISUALIZER_MODE = "visualizer_mode"

    // Default Values
    const val DEFAULT_SERVER_URL = "wss://gps-tracker-server-production-5900.up.railway.app"
    const val DEFAULT_MIN_SPEED = 22.0
    const val DEFAULT_AUTO_CENTER = true
    const val DEFAULT_VOICE_ENABLED = false
    const val DEFAULT_VISUALIZER_MODE = false

    // Location
    const val LOCATION_UPDATE_INTERVAL = 2000L // 2 seconds - reduced frequency for better accuracy
    const val LOCATION_MIN_UPDATE_INTERVAL = 2000L // 2 seconds - prevents GPS noise from high-frequency updates
    const val LOCATION_MAX_UPDATE_DELAY = 4000L
    const val LOCATION_MIN_DISPLACEMENT = 5.0f // 5 meters - minimum distance for location update

    // Speed
    const val MAX_SPEED_READINGS = 20
    const val MS_TO_KMH = 3.6
    const val MIN_SPEED_THRESHOLD = 1.5 // km/h - speeds below this are considered stationary
    const val MAX_GPS_ACCURACY = 20.0f // meters - reject GPS readings with accuracy worse than this

    // Database
    const val MAX_TRACK_POINTS_PER_SESSION = 10000
    const val OLD_DATA_RETENTION_DAYS = 30
}
