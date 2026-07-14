package com.tracker.gps.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.tracker.gps.MainActivity
import com.tracker.gps.R
import com.tracker.gps.api.SpeedHistoryApi
import com.tracker.gps.api.SpeedHistoryRecord
import com.tracker.gps.model.UserData
import com.tracker.gps.websocket.GPSWebSocketClient
import com.tracker.gps.shared.util.Constants
import com.tracker.gps.shared.util.SessionSpeedStats
import com.tracker.gps.shared.util.SpeedCalculator
import java.net.URI
import java.util.Locale

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var webSocketClient: GPSWebSocketClient? = null

    private var userId: String = ""
    private var userName: String = ""
    private var groupName: String = ""
    private var serverUrl: String = ""

    private var currentSpeed: Double = 0.0

    // Rolling speed statistics (avg / 10s / 500m). Pure math lives in the shared
    // module so it is unit-testable; see SessionSpeedStats.
    private val speedStats = SessionSpeedStats()

    // Previous accepted fix, used to measure the distance delta between fixes.
    // Kept separate from lastLocation (the most recent fix) so the 500m window
    // is not measured against the same point.
    private var previousLocation: Location? = null

    // FIT track recording
    private val sessionTrack = mutableListOf<Pair<Location, Long>>()

    // Voice announcement settings
    private var textToSpeech: TextToSpeech? = null
    private var lastAnnouncedSpeed: Int = -1
    private var lastAnnouncementTime: Long = 0
    private val announcementCooldownMs = 3000L // 3 seconds between announcements

    private var lastLocation: Location? = null
    private val userTracks = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

    private val jumpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.tracker.gps.ACTION_JUMP_DETECTED") {
                val maxHeight = intent.getDoubleExtra("maxHeight", 0.0)
                val hangtime = intent.getLongExtra("hangtime", 0L)
                webSocketClient?.sendGroupJump(userId, userName, groupName, maxHeight, hangtime)
            }
        }
    }

    var serviceListener: ServiceListener? = null

    interface ServiceListener {
        fun onSpeedUpdate(current: Double, max: Double, avg: Double, avg10s: Double, max10s: Double, avg500m: Double, max500m: Double)
        fun onLocationUpdate(location: Location)
        fun onUsersUpdate(users: List<UserData>)
        fun onConnectionStatusChanged(connected: Boolean)
        fun onGpsStatusChanged(active: Boolean)
        fun onGroupHorn(senderId: String, senderName: String)
        fun onGroupJump(senderId: String, senderName: String, maxHeight: Double, hangtime: Long)
        fun onError(message: String)
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        initializeTextToSpeech()

        val filter = IntentFilter("com.tracker.gps.ACTION_JUMP_DETECTED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(jumpReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(jumpReceiver, filter)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startTracking(userId: String, userName: String, groupName: String, serverUrl: String) {
        this.userId = userId
        this.userName = userName
        this.groupName = groupName
        this.serverUrl = serverUrl

        startForeground(NOTIFICATION_ID, createNotification(0.0))
        startLocationUpdates()
        connectWebSocket()
    }

    fun stopTracking() {
        // Submit max speed record if we have a valid max speed and location
        if (speedStats.max > 0 && lastLocation != null) {
            submitMaxSpeedRecord()
        }

        stopLocationUpdates()
        disconnectWebSocket()
        stopForeground(true)
        stopSelf()
    }

    private fun submitMaxSpeedRecord() {
        lastLocation?.let { location ->
            val speedHistoryApi = SpeedHistoryApi(serverUrl)
            val record = SpeedHistoryRecord(
                userId = userId,
                userName = userName,
                groupName = groupName,
                maxSpeed = speedStats.max,
                maxSpeed10s = speedStats.max10s,
                maxSpeed500m = speedStats.max500m,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis()
            )

            speedHistoryApi.submitSpeedHistory(
                record = record,
                onSuccess = {
                    Log.d(TAG, "Max speed record submitted successfully: ${speedStats.max} km/h")
                },
                onError = { error ->
                    Log.e(TAG, "Failed to submit max speed record: $error")
                }
            )
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_MIN_UPDATE_INTERVAL)
            setMaxUpdateDelayMillis(Constants.LOCATION_MAX_UPDATE_DELAY)
            setMinUpdateDistanceMeters(Constants.LOCATION_MIN_DISPLACEMENT)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        serviceListener?.onGpsStatusChanged(true)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceListener?.onGpsStatusChanged(false)
    }

    private fun handleLocationUpdate(location: Location) {
        // Filter out inaccurate GPS readings (>= to be stricter)
        if (location.hasAccuracy() && location.accuracy >= Constants.MAX_GPS_ACCURACY) {
            Log.w(TAG, "❌ REJECTED GPS reading: accuracy=${location.accuracy}m (threshold: ${Constants.MAX_GPS_ACCURACY}m)")
            return
        }

        Log.d(TAG, "✓ Accepted GPS reading: accuracy=${location.accuracy}m")

        // Distance/time since the PREVIOUS accepted fix (0 for the first fix).
        // Measured before updating lastLocation so the 500m window is not measured
        // against the same point (previously always 0).
        val now = System.currentTimeMillis()
        val prev = previousLocation
        val distanceMeters = prev?.distanceTo(location)?.toDouble() ?: 0.0
        val elapsedMs = if (prev != null) location.time - prev.time else 0L

        // Calculate speed in km/h, falling back to distance/time when the fix has
        // no hardware speed (matches the web client).
        val rawSpeed = SpeedCalculator.deriveSpeedKmh(location.hasSpeed(), location.speed, distanceMeters, elapsedMs)

        // Apply minimum speed threshold to filter out GPS noise when stationary
        currentSpeed = if (rawSpeed < Constants.MIN_SPEED_THRESHOLD) {
            0.0
        } else {
            rawSpeed
        }

        // Feed the pure rolling-stats calculator (avg / 10s / 500m + peaks)
        val stats = speedStats.update(rawSpeed, currentSpeed, now, distanceMeters)

        previousLocation = location
        lastLocation = location

        // Record for FIT track
        sessionTrack.add(Pair(location, now))

        // Update notification
        val notification = createNotification(currentSpeed)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Notify listeners
        serviceListener?.onSpeedUpdate(currentSpeed, stats.max, stats.avg, stats.avg10s, stats.max10s, stats.avg500m, stats.max500m)
        serviceListener?.onLocationUpdate(location)

        // Voice announcement
        checkAndAnnounceSpeed(currentSpeed)

        // Send to WebSocket (only if not in visualizer mode)
        val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        val visualizerMode = prefs.getBoolean(getString(R.string.pref_visualizer_mode_key), false)

        if (!visualizerMode) {
            webSocketClient?.let {
                if (it.isOpen) {
                    val bearing = if (location.hasBearing()) location.bearing else 0f
                    it.sendSpeed(userId, userName, groupName, currentSpeed, stats.max, location.latitude, location.longitude, bearing)
                }
            }
            // Note: the speed-history record is submitted once, on stopTracking().
            // Mid-session submission was removed because it created duplicate session
            // rows (one per max-speed increase), inflating the aggregate statistics.
        }

        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}, Speed: $currentSpeed km/h")
    }

    private fun connectWebSocket() {
        try {
            val uri = URI(serverUrl)
            webSocketClient = GPSWebSocketClient(uri, object : GPSWebSocketClient.WebSocketListener {
                override fun onConnected() {
                    webSocketClient?.sendRegister(userId, userName, groupName)
                    serviceListener?.onConnectionStatusChanged(true)
                }

                override fun onDisconnected() {
                    serviceListener?.onConnectionStatusChanged(false)
                }

                override fun onUsersUpdate(users: List<UserData>) {
                    // Update tracks
                    users.forEach { user ->
                        if (!userTracks.containsKey(user.userId)) {
                            userTracks[user.userId] = mutableListOf()
                        }
                        userTracks[user.userId]?.add(Pair(user.latitude, user.longitude))
                    }
                    serviceListener?.onUsersUpdate(users)
                }

                override fun onGroupHorn(senderId: String, senderName: String) {
                    serviceListener?.onGroupHorn(senderId, senderName)
                }

                override fun onGroupJump(senderId: String, senderName: String, maxHeight: Double, hangtime: Long) {
                    serviceListener?.onGroupJump(senderId, senderName, maxHeight, hangtime)
                }

                override fun onError(error: String) {
                    serviceListener?.onError(error)
                }
            })
            webSocketClient?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket connection error", e)
            serviceListener?.onError("Failed to connect: ${e.message}")
        }
    }

    private fun disconnectWebSocket() {
        webSocketClient?.closeManually()
        webSocketClient = null
    }

    fun resetStatistics() {
        speedStats.reset()
        previousLocation = null
        sessionTrack.clear()
    }

    fun getSessionTrack(): List<Pair<Location, Long>> = sessionTrack

    fun sendGroupHorn() {
        webSocketClient?.sendGroupHorn(userId, userName, groupName)
    }

    fun clearTracks() {
        userTracks.clear()
    }

    fun getUserTracks(): Map<String, List<Pair<Double, Double>>> {
        return userTracks
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                Log.d(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
            }
        }
    }

    private fun checkAndAnnounceSpeed(speed: Double) {
        val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        val voiceEnabled = prefs.getBoolean(getString(R.string.pref_voice_enabled_key), false)
        if (!voiceEnabled) return

        val speedUnit = prefs.getString(getString(R.string.pref_speed_unit_key), "kmh")
        val convertedSpeed = when (speedUnit) {
            "mph" -> speed * Constants.KMH_TO_MPH
            "knots" -> speed * Constants.KMH_TO_KNOTS
            else -> speed
        }

        val minSpeed = prefs.getFloat(getString(R.string.pref_voice_min_speed_key), 22f).toDouble()
        if (convertedSpeed < minSpeed) return

        val currentTime = System.currentTimeMillis()
        val speedInt = convertedSpeed.toInt()

        // Only announce if speed changed by at least 1 km/h and cooldown period passed
        if (speedInt != lastAnnouncedSpeed &&
            (currentTime - lastAnnouncementTime) >= announcementCooldownMs) {
            announceSpeed(speedInt)
            lastAnnouncedSpeed = speedInt
            lastAnnouncementTime = currentTime
        }
    }

    private fun announceSpeed(speed: Int) {
        textToSpeech?.let { tts ->
            if (tts.isSpeaking) {
                tts.stop()
            }
            val announcement = speed.toString()
            tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "Announcing speed: $speed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
        unregisterReceiver(jumpReceiver)
        Log.d(TAG, "Service destroyed, TextToSpeech shut down")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing GPS tracking notification"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(speedInKmh: Double): Notification {
        val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        val speedUnit = prefs.getString(getString(R.string.pref_speed_unit_key), "kmh")
        val convertedSpeed = when (speedUnit) {
            "mph" -> speedInKmh * Constants.KMH_TO_MPH
            "knots" -> speedInKmh * Constants.KMH_TO_KNOTS
            else -> speedInKmh
        }
        val unitLabel = when (speedUnit) {
            "mph" -> "mph"
            "knots" -> "knots"
            else -> "km/h"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_active))
            .setContentText(String.format("Velocidad: %.1f %s", convertedSpeed, unitLabel))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
