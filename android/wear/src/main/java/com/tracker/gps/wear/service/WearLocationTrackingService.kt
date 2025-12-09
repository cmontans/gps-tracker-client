package com.tracker.gps.wear.service

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
import com.tracker.gps.shared.Constants
import com.tracker.gps.wear.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class WearLocationTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentSpeed: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var speedReadings = mutableListOf<Double>()

    private var lastLocation: Location? = null
    private var isTracking = false

    var serviceListener: ServiceListener? = null

    interface ServiceListener {
        fun onSpeedUpdate(current: Double, max: Double, avg: Double)
        fun onLocationUpdate(location: Location)
        fun onGpsStatusChanged(active: Boolean)
    }

    inner class LocalBinder : Binder() {
        fun getService(): WearLocationTrackingService = this@WearLocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startTracking() {
        if (isTracking) return

        isTracking = true
        startForeground(Constants.NOTIFICATION_ID, createNotification(0.0))
        startLocationUpdates()
    }

    fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        stopLocationUpdates()
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
            Constants.LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL_MS)
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
            (location.speed * Constants.MS_TO_KMH).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // Update max speed
        if (currentSpeed > maxSpeed) {
            maxSpeed = currentSpeed
        }

        // Update average speed
        speedReadings.add(currentSpeed)
        if (speedReadings.size > Constants.AVERAGE_WINDOW_SIZE) {
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
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)

        // Notify listeners
        serviceListener?.onSpeedUpdate(currentSpeed, maxSpeed, avgSpeed)
        serviceListener?.onLocationUpdate(location)

        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}, Speed: $currentSpeed km/h")
    }

    fun resetStatistics() {
        maxSpeed = 0.0
        speedReadings.clear()
    }

    fun getCurrentSpeed() = currentSpeed
    fun getMaxSpeed() = maxSpeed
    fun getAvgSpeed() = if (speedReadings.isNotEmpty()) speedReadings.average() else 0.0

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing GPS tracking notification"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(speed: Double) =
        NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GPS Tracking")
            .setContentText(String.format("Speed: %.1f km/h", speed))
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

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "WearLocationService"
    }
}
