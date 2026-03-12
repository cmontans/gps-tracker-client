package com.tracker.gps.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tracker.gps.MainActivity
import com.tracker.gps.R
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.tracker.gps.db.AppDatabase
import com.tracker.gps.model.JumpSession
import com.tracker.gps.shared.util.JumpDetector
import com.tracker.gps.shared.util.JumpSensitivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.tracker.gps.service.WearableDataService
import com.tracker.gps.shared.model.JumpState

class JumpTrackingService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var jumpDetector: JumpDetector
    private lateinit var wearableDataService: WearableDataService
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentAltitude = MutableStateFlow(0.0)
    val currentAltitude = _currentAltitude.asStateFlow()

    private val _lastJumpHeight = MutableStateFlow(0.0)
    val lastJumpHeight = _lastJumpHeight.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _isCurrentlyJumping = MutableStateFlow(false)
    val isCurrentlyJumping = _isCurrentlyJumping.asStateFlow()

    private var textToSpeech: TextToSpeech? = null

    inner class LocalBinder : Binder() {
        fun getService(): JumpTrackingService = this@JumpTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }

        wearableDataService = WearableDataService(this)
        jumpDetector = JumpDetector(
            context = this,
            onJumpDetected = { height, hangtime ->
                handleJumpDetected(height, hangtime)
            },
            onAltitudeUpdate = { altitude, isJmp ->
                _currentAltitude.value = altitude
                if (_isCurrentlyJumping.value != isJmp) {
                    _isCurrentlyJumping.value = isJmp
                }
                notifyWearable()
            }
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_isTracking.value) return

        Log.d(TAG, "Starting jump tracking service")
        acquireWakeLock()
        
        val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        val sensitivityStr = prefs.getString("jump_sensitivity", "MEDIA") ?: "MEDIA"
        jumpDetector.sensitivity = try {
            JumpSensitivity.valueOf(sensitivityStr)
        } catch (e: Exception) {
            JumpSensitivity.MEDIA
        }
        
        val notification = createNotification("Tracking Kitesurf Jumps...")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        jumpDetector.start()
        _isTracking.value = true
    }

    private fun stopTracking() {
        if (!_isTracking.value) return

        Log.d(TAG, "Stopping jump tracking service")
        jumpDetector.stop()
        releaseWakeLock()
        
        _isTracking.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleJumpDetected(height: Double, hangtime: Long) {
        _lastJumpHeight.value = height
        
        serviceScope.launch {
            val jump = JumpSession(
                timestamp = System.currentTimeMillis(),
                maxHeight = height,
                hangtime = hangtime
            )
            AppDatabase.getDatabase(applicationContext).jumpDao().insertJump(jump)
            Log.d(TAG, "Saved jump: ${height}m, ${hangtime}ms")
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            val notificationText = "%.1f".format(height)
            notificationManager.notify(NOTIFICATION_ID, createNotification("Last Jump: $notificationText m"))
            
            // Announce height via TTS
            textToSpeech?.speak("$notificationText", TextToSpeech.QUEUE_FLUSH, null, null)

            notifyWearable()
        }
    }

    private fun notifyWearable() {
        if (!_isTracking.value) return
        val state = JumpState(
            currentAltitude = _currentAltitude.value,
            lastJumpHeight = _lastJumpHeight.value,
            isCurrentlyJumping = _isCurrentlyJumping.value,
            timestamp = System.currentTimeMillis()
        )
        wearableDataService.sendJumpState(state)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JumpTracker:WakeLock").apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    fun setSensitivity(sensitivity: JumpSensitivity) {
        if (::jumpDetector.isInitialized) {
            jumpDetector.sensitivity = sensitivity
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jump Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kitesurfing jump height tracking"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jump Tracker Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        textToSpeech?.shutdown()
        wearableDataService.cleanup()
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "JumpTrackingService"
        private const val CHANNEL_ID = "jump_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
    }
}
