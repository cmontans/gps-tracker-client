package com.tracker.gps.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.tracker.gps.MainActivity
import com.tracker.gps.R
import com.tracker.gps.model.UserData
import com.tracker.gps.websocket.GPSWebSocketClient
import java.net.URI

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

    private var lastLocation: Location? = null
    private val userTracks = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

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
        stopLocationUpdates()
        disconnectWebSocket()
        stopForeground(true)
        stopSelf()
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
            1000L // 1 second interval
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMaxUpdateDelayMillis(2000L)
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
        lastLocation = location

        // Calculate speed in km/h
        currentSpeed = if (location.hasSpeed()) {
            (location.speed * 3.6).coerceAtLeast(0.0) // Convert m/s to km/h
        } else {
            0.0
        }

        // Update max speed
        if (currentSpeed > maxSpeed) {
            maxSpeed = currentSpeed
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

        // Send to WebSocket (only if NOT in visualizer mode)
        val prefs = getSharedPreferences("gps_tracker_prefs", MODE_PRIVATE)
        val visualizerMode = prefs.getBoolean(getString(R.string.pref_visualizer_mode_key), false)

        if (!visualizerMode) {
            webSocketClient?.let {
                if (it.isOpen) {
                    val bearing = if (location.hasBearing()) location.bearing else 0f
                    it.sendSpeed(userId, currentSpeed, location.latitude, location.longitude, bearing)
                }
            }
        } else {
            Log.d(TAG, "Visualizer mode enabled - location NOT sent to server")
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
