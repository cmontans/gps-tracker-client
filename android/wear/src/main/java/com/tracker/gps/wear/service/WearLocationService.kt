package com.tracker.gps.wear.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import com.tracker.gps.shared.util.SpeedCalculator
import com.tracker.gps.wear.MainActivity
import com.tracker.gps.wear.R
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import android.speech.tts.TextToSpeech
import com.tracker.gps.shared.util.JumpDetector

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

    private var currentSpeed = 0.0
    private var maxSpeed = 0.0
    private val speedReadings = mutableListOf<Double>()
    private var previousLocation: Location? = null
    private var currentAltitude = 0.0
    private var lastJumpHeight = 0.0
    private var sessionMaxJumpHeight = 0.0
    private val jumpHistory = mutableListOf<Double>()
    private var isCurrentlyJumping = false
    private var hasGps = false
    private var isConnected = false

    // Users
    private val users = mutableListOf<UserData>()

    // Standalone Jump Detector
    private lateinit var jumpDetector: JumpDetector

    private var textToSpeech: TextToSpeech? = null
    private var lastAnnouncedSpeed: Int = -1
    private var lastAnnouncementTime: Long = 0
    private val announcementCooldownMs = 3000L

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
        initializeTextToSpeech()

        jumpDetector = JumpDetector(
            context = this,
            onJumpDetected = { height, hangtime ->
                // Handled standalone jump, we can print it
                lastJumpHeight = height
                if (height > sessionMaxJumpHeight) {
                    sessionMaxJumpHeight = height
                }
                jumpHistory.add(0, height) // Newest first
                notifyTrackingState()

                // Announce height via TTS
                textToSpeech?.let { tts ->
                    if (tts.isSpeaking) {
                        tts.stop()
                    }
                    val heightText = "%.1f".format(height)
                    tts.speak(heightText, TextToSpeech.QUEUE_FLUSH, null, "wear_jump_tts")
                }

                // Bring MainActivity to the foreground and wake the screen
                try {
                    val startAppIntent = Intent(this@WearLocationService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(startAppIntent)
                } catch (e: Exception) {
                    android.util.Log.e("WearLocationService", "Failed to start MainActivity on jump", e)
                }
                
                // Sync jump record to phone for history
                serviceScope.launch {
                    try {
                        val record = JumpRecordSync(
                            maxHeight = height,
                            hangtime = hangtime,
                            timestamp = System.currentTimeMillis()
                        )
                        val dataBytes = DataSerializer.toBytes(record)
                        
                        connectedNodeId?.let { nodeId ->
                            messageClient.sendMessage(nodeId, WearPaths.JUMP_RECORD_SYNC, dataBytes).await()
                        } ?: run {
                            // If no connection, find connected nodes
                            val capability = capabilityClient.getCapability(
                                Constants.CAPABILITY_TRACKER_APP,
                                CapabilityClient.FILTER_REACHABLE
                            ).await()
                            
                            capability.nodes.firstOrNull()?.id?.let { nodeId ->
                                connectedNodeId = nodeId
                                messageClient.sendMessage(nodeId, WearPaths.JUMP_RECORD_SYNC, dataBytes).await()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WearLocationService", "Error syncing jump to phone", e)
                    }
                }
            },
            onAltitudeUpdate = { altitude, isJmp ->
                if (isStandaloneMode) {
                    currentAltitude = altitude
                    isCurrentlyJumping = isJmp
                    notifyTrackingState()
                }
            }
        )

        // Check for phone connectivity
        serviceScope.launch {
            checkPhoneConnection()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // When service is started via startForegroundService, we must call startForeground
        // to avoid a crash. We do this with a basic notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // Start tracking from the intent so the first tap after launch isn't a no-op
        // when the Activity hasn't finished binding yet (startTracking is idempotent).
        val uname = intent?.getStringExtra(EXTRA_USER_NAME)
        if (!uname.isNullOrEmpty() && !isTracking) {
            startTracking(uname, intent.getStringExtra(EXTRA_GROUP_NAME) ?: "")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        try {
            messageClient.removeListener(messageListener)
        } catch (e: Exception) {
            android.util.Log.w("WearLocationService", "Error removing data-layer listener", e)
        }
        textToSpeech?.shutdown()
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
        lastAnnouncedSpeed = -1
        lastAnnouncementTime = 0

        prefs.edit().apply {
            putString(Constants.PREF_USER_NAME, userName)
            putString(Constants.PREF_GROUP_NAME, groupName)
            apply()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startLocationUpdates()
        jumpDetector.start()

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
        jumpDetector.stop()
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
        previousLocation = null
        sessionMaxJumpHeight = 0.0
        jumpHistory.clear()
        lastAnnouncedSpeed = -1
        lastAnnouncementTime = 0
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

    // Held as a field so it can be removed in onDestroy (an anonymous listener
    // would leak the service and keep firing callbacks after teardown).
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        try {
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
                WearPaths.JUMP_UPDATE -> {
                    val jumpState = DataSerializer.fromBytes<JumpState>(messageEvent.data)
                    handleJumpStateUpdate(jumpState)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WearLocationService", "Error handling data-layer message", e)
        }
    }

    private fun setupDataLayerListeners() {
        messageClient.addListener(messageListener)
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
        // Filter out inaccurate GPS readings (>= to be stricter)
        if (location.hasAccuracy() && location.accuracy >= Constants.WEAR_MAX_GPS_ACCURACY) {
            android.util.Log.w("WearLocationService", "❌ REJECTED GPS reading: accuracy=${location.accuracy}m (threshold: ${Constants.WEAR_MAX_GPS_ACCURACY}m)")
            return
        }

        hasGps = true
        listener?.onGpsStatusChanged(true)

        // Distance/time since the previous accepted fix, for the speed fallback.
        val prev = previousLocation
        val distanceMeters = prev?.distanceTo(location)?.toDouble() ?: 0.0
        val elapsedMs = if (prev != null) location.time - prev.time else 0L
        previousLocation = location

        // Calculate speed in km/h, falling back to distance/time when the fix has
        // no hardware speed (matches the web and phone clients).
        val rawSpeed = SpeedCalculator.deriveSpeedKmh(location.hasSpeed(), location.speed, distanceMeters, elapsedMs)

        // Apply minimum speed threshold to filter out GPS noise when stationary
        currentSpeed = if (rawSpeed < Constants.MIN_SPEED_THRESHOLD) {
            0.0
        } else {
            rawSpeed
        }

        if (rawSpeed > maxSpeed) {
            maxSpeed = rawSpeed
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

        checkAndAnnounceSpeed(currentSpeed)

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

    private fun handleJumpStateUpdate(state: JumpState) {
        currentAltitude = state.currentAltitude
        lastJumpHeight = state.lastJumpHeight
        isCurrentlyJumping = state.isCurrentlyJumping
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
            avgSpeed = avgSpeed,
            currentAltitude = currentAltitude,
            lastJumpHeight = lastJumpHeight,
            sessionMaxJumpHeight = sessionMaxJumpHeight,
            jumpHistory = jumpHistory.toList(),
            isCurrentlyJumping = isCurrentlyJumping
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

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                android.util.Log.d("WearLocationService", "TextToSpeech initialized successfully")
            } else {
                android.util.Log.e("WearLocationService", "TextToSpeech initialization failed")
            }
        }
    }

    private fun checkAndAnnounceSpeed(speed: Double) {
        val voiceEnabled = prefs.getBoolean(Constants.PREF_VOICE_ENABLED, Constants.DEFAULT_VOICE_ENABLED)
        if (!voiceEnabled) return

        val minSpeed = prefs.getFloat(Constants.PREF_VOICE_MIN_SPEED, Constants.DEFAULT_MIN_SPEED.toFloat()).toDouble()
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
            tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "wear_speed_tts")
            android.util.Log.d("WearLocationService", "Announcing speed: $speed")
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
            .setSmallIcon(R.drawable.ic_stat_speed)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIFICATION_ID = 1

        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_GROUP_NAME = "extra_group_name"
    }
}
