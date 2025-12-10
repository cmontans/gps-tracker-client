package com.tracker.gps.wear.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.wearable.*
import com.tracker.gps.shared.model.*
import com.tracker.gps.shared.util.Constants
import com.tracker.gps.shared.util.DataSerializer
import com.tracker.gps.wear.MainActivity
import com.tracker.gps.wear.R
import kotlinx.coroutines.*
import java.util.*

class WearLocationService : Service() {
    private val binder = LocalBinder()
    private var listener: ServiceListener? = null

    // Coroutine scope
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Wearable Data Layer
    private lateinit var dataClient: DataClient
    private lateinit var messageClient: MessageClient
    private lateinit var capabilityClient: CapabilityClient

    // State
    private var isTracking = false
    private var userId: String = ""
    private var userName: String = ""
    private var groupName: String = ""
    private var isStandaloneMode = false
    private var connectedNodeId: String? = null

    // Speed tracking
    private var currentSpeed = 0.0
    private var maxSpeed = 0.0
    private val speedReadings = mutableListOf<Double>()
    private var hasGps = false
    private var isConnected = false

    // Users
    private val users = mutableListOf<UserData>()

    // WebSocket (for standalone mode)
    private var webSocketClient: org.java_websocket.client.WebSocketClient? = null

    // SharedPreferences
    private lateinit var prefs: SharedPreferences

    inner class LocalBinder : Binder() {
        fun getService(): WearLocationService = this@WearLocationService
    }

    interface ServiceListener {
        fun onTrackingStateChanged(state: TrackingState)
        fun onUsersUpdate(users: List<UserData>)
        fun onConnectionStatusChanged(connected: Boolean)
        fun onGpsStatusChanged(active: Boolean)
    }

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        userId = prefs.getString(Constants.PREF_USER_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(Constants.PREF_USER_ID, it).apply()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dataClient = Wearable.getDataClient(this)
        messageClient = Wearable.getMessageClient(this)
        capabilityClient = Wearable.getCapabilityClient(this)

        createNotificationChannel()
        setupLocationCallback()
        setupDataLayerListeners()

        // Check for phone connectivity
        serviceScope.launch {
            checkPhoneConnection()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    fun setListener(listener: ServiceListener) {
        this.listener = listener
        notifyTrackingState()
    }

    fun startTracking(userName: String, groupName: String) {
        if (isTracking) return

        this.userName = userName
        this.groupName = groupName
        this.isTracking = true

        prefs.edit().apply {
            putString(Constants.PREF_USER_NAME, userName)
            putString(Constants.PREF_GROUP_NAME, groupName)
            apply()
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()

        // Determine mode and connect
        serviceScope.launch {
            if (connectedNodeId != null) {
                // Phone is connected, use companion mode
                isStandaloneMode = false
                sendControlCommand(WearControlCommand.StartTracking(userName, groupName))
            } else {
                // No phone, use standalone mode with direct WebSocket
                isStandaloneMode = true
                connectToServerStandalone()
            }

            notifyTrackingState()
        }
    }

    fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)

        if (isStandaloneMode) {
            disconnectFromServer()
        } else {
            sendControlCommand(WearControlCommand.StopTracking)
        }

        resetStats()
        notifyTrackingState()
    }

    fun triggerGroupHorn() {
        if (!isTracking) return

        if (isStandaloneMode) {
            // Send directly to server
            sendWebSocketMessage(WebSocketMessage.GroupHorn(userId = userId))
        } else {
            // Send via phone
            sendControlCommand(WearControlCommand.TriggerGroupHorn)
        }
    }

    fun resetStats() {
        maxSpeed = 0.0
        speedReadings.clear()
        notifyTrackingState()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun setupDataLayerListeners() {
        messageClient.addListener { messageEvent ->
            when (messageEvent.path) {
                WearPaths.TRACKING_STATE -> {
                    val state = DataSerializer.fromBytes<TrackingState>(messageEvent.data)
                    handleTrackingStateUpdate(state)
                }
                WearPaths.USERS_UPDATE -> {
                    val userList = DataSerializer.fromBytes<List<UserData>>(messageEvent.data)
                    handleUsersUpdate(userList)
                }
                WearPaths.CONNECTION_STATUS -> {
                    val status = DataSerializer.fromBytes<ConnectionStatus>(messageEvent.data)
                    isConnected = status.isConnected
                    listener?.onConnectionStatusChanged(isConnected)
                }
            }
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
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun handleLocationUpdate(location: Location) {
        hasGps = true
        listener?.onGpsStatusChanged(true)

        currentSpeed = if (location.hasSpeed()) {
            (location.speed * Constants.MS_TO_KMH).coerceAtLeast(0.0)
        } else {
            0.0
        }

        if (currentSpeed > maxSpeed) {
            maxSpeed = currentSpeed
        }

        speedReadings.add(currentSpeed)
        if (speedReadings.size > Constants.MAX_SPEED_READINGS) {
            speedReadings.removeAt(0)
        }

        val bearing = if (location.hasBearing()) location.bearing else 0f

        // Update notification
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Send location update
        if (isStandaloneMode) {
            sendWebSocketMessage(
                WebSocketMessage.Speed(
                    userId = userId,
                    speed = currentSpeed,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    bearing = bearing
                )
            )
        } else {
            sendLocationToPhone(location)
        }

        notifyTrackingState()
    }

    private fun sendLocationToPhone(location: Location) {
        connectedNodeId?.let { nodeId ->
            val data = DataSerializer.toBytes(
                mapOf(
                    "userId" to userId,
                    "speed" to currentSpeed,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "bearing" to location.bearing
                )
            )

            messageClient.sendMessage(nodeId, WearPaths.LOCATION_UPDATE, data)
        }
    }

    private fun sendControlCommand(command: WearControlCommand) {
        connectedNodeId?.let { nodeId ->
            val data = DataSerializer.toBytes(command)
            messageClient.sendMessage(nodeId, WearPaths.CONTROL_COMMAND, data)
        }
    }

    private suspend fun checkPhoneConnection() {
        val nodes = capabilityClient.getCapability(
            Constants.CAPABILITY_TRACKER_APP,
            CapabilityClient.FILTER_REACHABLE
        ).await()

        connectedNodeId = nodes.nodes.firstOrNull()?.id
        isStandaloneMode = connectedNodeId == null
    }

    private fun connectToServerStandalone() {
        // TODO: Implement WebSocket connection for standalone mode
        // This would use the same WebSocket client as the phone app
    }

    private fun disconnectFromServer() {
        webSocketClient?.close()
        webSocketClient = null
    }

    private fun sendWebSocketMessage(message: Any) {
        // TODO: Implement WebSocket message sending
    }

    private fun handleTrackingStateUpdate(state: TrackingState) {
        currentSpeed = state.currentSpeed
        maxSpeed = state.maxSpeed
        notifyTrackingState()
    }

    private fun handleUsersUpdate(userList: List<UserData>) {
        users.clear()
        users.addAll(userList)
        listener?.onUsersUpdate(users)
    }

    private fun notifyTrackingState() {
        val avgSpeed = if (speedReadings.isNotEmpty()) {
            speedReadings.average()
        } else {
            0.0
        }

        val state = TrackingState(
            isTracking = isTracking,
            userName = userName,
            groupName = groupName,
            currentSpeed = currentSpeed,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed
        )

        listener?.onTrackingStateChanged(state)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for GPS tracking service"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Speed: %.1f km/h".format(currentSpeed))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
