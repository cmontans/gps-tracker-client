package com.tracker.gps.wear

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tracker.gps.shared.Constants
import com.tracker.gps.wear.databinding.ActivityMainBinding
import com.tracker.gps.wear.service.WearLocationTrackingService
import com.tracker.gps.wear.wearable.WearDataLayerService
import com.tracker.gps.wear.wearable.WearDataListenerService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var locationService: WearLocationTrackingService? = null
    private var serviceBound = false
    private lateinit var dataLayerService: WearDataLayerService
    private lateinit var sharedPreferences: SharedPreferences

    private var isTracking = false
    private var userId = ""
    private var userName = ""
    private var groupName = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as WearLocationTrackingService.LocalBinder
            locationService = localBinder.getService()
            serviceBound = true

            locationService?.serviceListener = object : WearLocationTrackingService.ServiceListener {
                override fun onSpeedUpdate(current: Double, max: Double, avg: Double) {
                    updateSpeedDisplay(current, max, avg)
                }

                override fun onLocationUpdate(location: Location) {
                    // Handle location updates if needed
                }

                override fun onGpsStatusChanged(active: Boolean) {
                    updateGpsStatus(active)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            serviceBound = false
        }
    }

    private val dataLayerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WearDataListenerService.ACTION_SPEED_UPDATE -> {
                    val current = intent.getDoubleExtra(Constants.KEY_CURRENT_SPEED, 0.0)
                    val max = intent.getDoubleExtra(Constants.KEY_MAX_SPEED, 0.0)
                    val avg = intent.getDoubleExtra(Constants.KEY_AVG_SPEED, 0.0)
                    updateSpeedDisplay(current, max, avg)
                }
                WearDataListenerService.ACTION_CONNECTION_STATUS -> {
                    val connected = intent.getBooleanExtra(Constants.KEY_CONNECTED, false)
                    val gpsActive = intent.getBooleanExtra(Constants.KEY_GPS_ACTIVE, false)
                    updateConnectionStatus(connected)
                    updateGpsStatus(gpsActive)
                }
                WearDataListenerService.ACTION_TRACKING_STATE -> {
                    val trackingActive = intent.getBooleanExtra(Constants.KEY_TRACKING_ACTIVE, false)
                    isTracking = trackingActive
                    updateTrackingButton()
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startTracking()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        dataLayerService = WearDataLayerService(this)

        loadSettings()
        setupUI()
        registerDataLayerReceiver()
    }

    private fun loadSettings() {
        userId = sharedPreferences.getString("user_id", "") ?: ""
        userName = sharedPreferences.getString("user_name", "Watch User") ?: "Watch User"
        groupName = sharedPreferences.getString("group_name", "default") ?: "default"

        // Generate user ID if not exists
        if (userId.isEmpty()) {
            userId = java.util.UUID.randomUUID().toString()
            sharedPreferences.edit().putString("user_id", userId).apply()
        }
    }

    private fun setupUI() {
        binding.startStopButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.resetButton.setOnClickListener {
            locationService?.resetStatistics()
            dataLayerService.sendResetStatsMessage()
            Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show()
        }

        binding.hornButton.setOnClickListener {
            dataLayerService.sendGroupHornMessage()
            Toast.makeText(this, "Horn sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            startTracking()
        } else {
            permissionLauncher.launch(denied.toTypedArray())
        }
    }

    private fun startTracking() {
        isTracking = true
        updateTrackingButton()

        // Start local service
        val serviceIntent = Intent(this, WearLocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Notify phone to start tracking
        dataLayerService.sendStartTrackingMessage(userId, userName, groupName)

        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        isTracking = false
        updateTrackingButton()

        // Stop local service
        locationService?.stopTracking()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }

        // Notify phone to stop tracking
        dataLayerService.sendStopTrackingMessage()

        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateSpeedDisplay(current: Double, max: Double, avg: Double) {
        runOnUiThread {
            binding.currentSpeedText.text = String.format("%.1f", current)
            binding.maxSpeedText.text = String.format("%.1f", max)
            binding.avgSpeedText.text = String.format("%.1f", avg)
        }
    }

    private fun updateGpsStatus(active: Boolean) {
        runOnUiThread {
            binding.gpsStatus.text = if (active) getString(R.string.gps_active) else getString(R.string.gps_inactive)
            binding.gpsStatus.setTextColor(
                if (active) getColor(android.R.color.holo_green_light)
                else getColor(android.R.color.holo_red_light)
            )
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        runOnUiThread {
            binding.connectionStatus.text = if (connected) getString(R.string.connected) else getString(R.string.disconnected)
            binding.connectionStatus.setTextColor(
                if (connected) getColor(android.R.color.holo_green_light)
                else getColor(android.R.color.holo_red_light)
            )
        }
    }

    private fun updateTrackingButton() {
        runOnUiThread {
            binding.startStopButton.text = if (isTracking) getString(R.string.stop_tracking) else getString(R.string.start_tracking)
        }
    }

    private fun registerDataLayerReceiver() {
        val filter = IntentFilter().apply {
            addAction(WearDataListenerService.ACTION_SPEED_UPDATE)
            addAction(WearDataListenerService.ACTION_CONNECTION_STATUS)
            addAction(WearDataListenerService.ACTION_TRACKING_STATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(dataLayerReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataLayerReceiver)
    }

    companion object {
        private const val TAG = "WearMainActivity"
    }
}
