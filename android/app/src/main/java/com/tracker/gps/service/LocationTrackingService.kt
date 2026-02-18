package com.tracker.gps.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
    private var maxSpeed: Double = 0.0
    private var speedReadings = mutableListOf<Double>()
    private val maxSpeedReadings = 20

    // Voice announcement settings
    private var textToSpeech: TextToSpeech? = null
    private var lastAnnouncedSpeed: Int = -1
    private var lastAnnouncementTime: Long = 0
    private val announcementCooldownMs = 3000L // 3 seconds between announcements

    private var lastLocation: Location? = null
    // ConcurrentHashMap avoids ConcurrentModificationException: userTracks is written from the
    // WebSocket callback thread and read from the UI thread (via getUserTracks()).
    private val userTracks = ConcurrentHashMap<String, MutableList<Pair<Double, Double>>>()

    var serviceListener: ServiceListener? = null

    interface ServiceListener {
        fun onSpeedUpdate(current: Double, max: Double, avg: Double)
        fun onLocationUpdate(location: Location)
        fun onUsersUpdate(users: List<UserData>)
        fun onConnectionStatusChanged(connected: Boolean)
        fun onGpsStatusChanged(active: Boolean)
        fun onGroupHorn()
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
        if (maxSpeed > 0 && lastLocation != null) {
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
                maxSpeed = maxSpeed,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis()
            )

            speedHistoryApi.submitSpeedHistory(
                record = record,
                onSuccess = {
                    Log.d(TAG, "Max speed record submitted successfully: $maxSpeed km/h")
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

        lastLocation = location

        // Calculate speed in km/h
        var rawSpeed = if (location.hasSpeed()) {
            (location.speed * Constants.MS_TO_KMH).coerceAtLeast(0.0) // Convert m/s to km/h
        } else {
            0.0
        }

        // Apply minimum speed threshold to filter out GPS noise when stationary
        currentSpeed = if (rawSpeed < Constants.MIN_SPEED_THRESHOLD) {
            0.0
        } else {
            rawSpeed
        }

        // Update max speed (use raw speed before threshold for max tracking)
        if (rawSpeed > maxSpeed) {
            maxSpeed = rawSpeed
        }

        // Update average speed
        speedReadings.add(currentSpeed)
        if (speedReadings.size > maxSpeedReadings) {
            speedReadings.removeAt(0)
        }
        val avgSpeed = if (speedReadings.isNotEmpty()) {
            speedReadings.average()
        } else {
            0.0
        }

        // Update notification
        val notification = createNotification(currentSpeed)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Notify listeners
        serviceListener?.onSpeedUpdate(currentSpeed, maxSpeed, avgSpeed)
        serviceListener?.onLocationUpdate(location)

        // Voice announcement
        checkAndAnnounceSpeed(currentSpeed)

        // Send to WebSocket (only if not in visualizer mode)
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val visualizerMode = prefs.getBoolean(getString(R.string.pref_visualizer_mode_key), false)

        if (!visualizerMode) {
            webSocketClient?.let {
                if (it.isOpen) {
                    val bearing = if (location.hasBearing()) location.bearing else 0f
                    it.sendSpeed(userId, userName, groupName, currentSpeed, maxSpeed, location.latitude, location.longitude, bearing)
                }
            }
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

                override fun onGroupHorn() {
                    serviceListener?.onGroupHorn()
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
        maxSpeed = 0.0
        speedReadings.clear()
    }

    fun sendGroupHorn() {
        webSocketClient?.sendGroupHorn(userId)
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
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val voiceEnabled = prefs.getBoolean(getString(R.string.pref_voice_enabled_key), false)
        val minSpeed = prefs.getFloat(getString(R.string.pref_voice_min_speed_key), 22f).toDouble()

        if (!voiceEnabled) return
        if (speed < minSpeed) return

        val currentTime = System.currentTimeMillis()
        val speedInt = speed.toInt()

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
            Log.d(TAG, "Announcing speed: $speed km/h")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
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

    private fun createNotification(speed: Double) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_active))
            .setContentText(getString(R.string.tracking_notification_text, speed))
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

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
