package com.tracker.gps

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.tracker.gps.shared.model.UserData
import com.tracker.gps.service.LocationTrackingService
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // UI Elements
    private lateinit var etUserName: TextInputEditText
    private lateinit var etGroupName: TextInputEditText
    private lateinit var btnStartStop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnToggleMap: Button
    private lateinit var btnFullscreenMap: Button
    private lateinit var btnGroupHorn: Button
    private lateinit var btnResetStats: Button
    private lateinit var btnClearTracks: Button
    private lateinit var tvCurrentSpeed: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvAvgSpeed: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var cardMap: MaterialCardView
    private lateinit var rvUsers: RecyclerView

    // Service
    private var trackingService: LocationTrackingService? = null
    private var serviceBound = false

    // Map
    private var googleMap: GoogleMap? = null
    private val userMarkers = mutableMapOf<String, Marker>()
    private val userPolylines = mutableMapOf<String, Polyline>()
    private var isMapVisible = false

    // Adapter
    private lateinit var usersAdapter: UsersAdapter

    // Wake Lock
    private var wakeLock: PowerManager.WakeLock? = null

    // Sound
    private lateinit var soundPool: SoundPool
    private var hornSoundId: Int = 0

    // State
    private var isTracking = false
    private var userId: String = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            trackingService = binder.getService()
            serviceBound = true

            trackingService?.serviceListener = object : LocationTrackingService.ServiceListener {
                override fun onSpeedUpdate(current: Double, max: Double, avg: Double) {
                    runOnUiThread {
                        tvCurrentSpeed.text = String.format("%.1f", current)
                        tvMaxSpeed.text = String.format("%.1f", max)
                        tvAvgSpeed.text = String.format("%.1f", avg)
                    }
                }

                override fun onLocationUpdate(location: Location) {
                    runOnUiThread {
                        updateMyLocationOnMap(location)
                    }
                }

                override fun onUsersUpdate(users: List<UserData>) {
                    runOnUiThread {
                        usersAdapter.updateUsers(users)
                        updateUsersOnMap(users)
                    }
                }

                override fun onConnectionStatusChanged(connected: Boolean) {
                    runOnUiThread {
                        updateConnectionStatus(connected)
                    }
                }

                override fun onGpsStatusChanged(active: Boolean) {
                    runOnUiThread {
                        updateGpsStatus(active)
                    }
                }

                override fun onGroupHorn() {
                    runOnUiThread {
                        playHornSound()
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            serviceBound = false
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkNotificationPermission()
                } else {
                    startTrackingService()
                }
            }
            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTrackingService()
        } else {
            Toast.makeText(
                this,
                getString(R.string.notification_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set dark mode as default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        setContentView(R.layout.activity_main)

        initViews()
        initMap()
        initSound()
        loadPreferences()
        setupClickListeners()
    }

    private fun initViews() {
        etUserName = findViewById(R.id.etUserName)
        etGroupName = findViewById(R.id.etGroupName)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnSettings = findViewById(R.id.btnSettings)
        btnToggleMap = findViewById(R.id.btnToggleMap)
        btnFullscreenMap = findViewById(R.id.btnFullscreenMap)
        btnGroupHorn = findViewById(R.id.btnGroupHorn)
        btnResetStats = findViewById(R.id.btnResetStats)
        btnClearTracks = findViewById(R.id.btnClearTracks)
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed)
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed)
        tvAvgSpeed = findViewById(R.id.tvAvgSpeed)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        cardMap = findViewById(R.id.cardMap)
        rvUsers = findViewById(R.id.rvUsers)

        usersAdapter = UsersAdapter()
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = usersAdapter
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // We'll generate a simple beep sound programmatically
        // In a production app, you would load from a resource file
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // Generate or load userId
        userId = prefs.getString(getString(R.string.pref_user_id_key), null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(getString(R.string.pref_user_id_key), newId).apply()
            newId
        }
    }

    private fun setupClickListeners() {
        btnStartStop.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                val userName = etUserName.text.toString().trim()
                val groupName = etGroupName.text.toString().trim()

                when {
                    userName.isEmpty() -> {
                        Toast.makeText(this, getString(R.string.enter_name), Toast.LENGTH_SHORT).show()
                    }
                    groupName.isEmpty() -> {
                        Toast.makeText(this, getString(R.string.enter_group), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        checkPermissionsAndStart()
                    }
                }
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnToggleMap.setOnClickListener {
            isMapVisible = !isMapVisible
            cardMap.visibility = if (isMapVisible) View.VISIBLE else View.GONE
            btnToggleMap.text = if (isMapVisible) getString(R.string.hide_map) else getString(R.string.view_map)
        }

        btnFullscreenMap.setOnClickListener {
            // TODO: Implement fullscreen map activity
            Toast.makeText(this, "Fullscreen map - coming soon", Toast.LENGTH_SHORT).show()
        }

        btnGroupHorn.setOnClickListener {
            trackingService?.sendGroupHorn()
        }

        btnResetStats.setOnClickListener {
            trackingService?.resetStatistics()
            tvMaxSpeed.text = "0.0"
            tvAvgSpeed.text = "0.0"
        }

        btnClearTracks.setOnClickListener {
            clearMapTracks()
        }
    }

    private fun checkPermissionsAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkNotificationPermission()
                } else {
                    startTrackingService()
                }
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startTrackingService()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun startTrackingService() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val serverUrl = prefs.getString(
            getString(R.string.pref_server_url_key),
            getString(R.string.default_server_url)
        ) ?: getString(R.string.default_server_url)

        val userName = etUserName.text.toString().trim()
        val groupName = etGroupName.text.toString().trim()

        val intent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Wait a bit for service to bind, then start tracking
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            trackingService?.startTracking(userId, userName, groupName, serverUrl)
            isTracking = true
            updateUIForTracking(true)
            acquireWakeLock()
        }, 500)
    }

    private fun stopTracking() {
        trackingService?.stopTracking()

        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }

        isTracking = false
        updateUIForTracking(false)
        releaseWakeLock()

        // Clear map
        userMarkers.clear()
        userPolylines.clear()
        googleMap?.clear()
    }

    private fun updateUIForTracking(tracking: Boolean) {
        btnStartStop.text = if (tracking) getString(R.string.stop_tracking) else getString(R.string.start_tracking)
        btnToggleMap.isEnabled = tracking
        btnFullscreenMap.isEnabled = tracking
        btnGroupHorn.isEnabled = tracking
        etUserName.isEnabled = !tracking
        etGroupName.isEnabled = !tracking
    }

    private fun updateConnectionStatus(connected: Boolean) {
        tvConnectionStatus.text = when {
            connected -> getString(R.string.status_connected)
            isTracking -> getString(R.string.status_connecting)
            else -> getString(R.string.status_disconnected)
        }

        val color = when {
            connected -> ContextCompat.getColor(this, R.color.status_connected)
            isTracking -> ContextCompat.getColor(this, R.color.status_connecting)
            else -> ContextCompat.getColor(this, R.color.status_disconnected)
        }
        tvConnectionStatus.setBackgroundColor(color)
    }

    private fun updateGpsStatus(active: Boolean) {
        tvGpsStatus.text = if (active) getString(R.string.gps_active) else getString(R.string.gps_searching)
        val color = if (active) {
            ContextCompat.getColor(this, R.color.gps_active)
        } else {
            ContextCompat.getColor(this, R.color.gps_searching)
        }
        tvGpsStatus.setBackgroundColor(color)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "GPSTracker::WakeLock"
        )
        wakeLock?.acquire(10*60*60*1000L) // 10 hours max
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun playHornSound() {
        // Play a system notification sound or vibrate
        Toast.makeText(this, "🔔 Group Horn!", Toast.LENGTH_SHORT).show()
        // In production, load and play actual horn sound
    }

    // Map Methods
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
        }
    }

    private fun updateMyLocationOnMap(location: Location) {
        googleMap?.let { map ->
            val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val autoCenterEnabled = prefs.getBoolean(
                getString(R.string.pref_auto_center_key),
                true
            )

            if (autoCenterEnabled) {
                val position = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            }
        }
    }

    private fun updateUsersOnMap(users: List<UserData>) {
        googleMap?.let { map ->
            // Remove markers for users no longer in the list
            val currentUserIds = users.map { it.userId }.toSet()
            userMarkers.keys.filter { it !in currentUserIds }.forEach { userId ->
                userMarkers[userId]?.remove()
                userMarkers.remove(userId)
                userPolylines[userId]?.remove()
                userPolylines.remove(userId)
            }

            // Update or create markers for current users
            users.forEach { user ->
                val position = LatLng(user.latitude, user.longitude)

                val marker = userMarkers[user.userId]
                if (marker != null) {
                    marker.position = position
                    marker.title = "${user.userName} - ${String.format("%.1f", user.speed)} km/h"
                    marker.rotation = user.bearing
                } else {
                    val newMarker = map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("${user.userName} - ${String.format("%.1f", user.speed)} km/h")
                            .rotation(user.bearing)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    newMarker?.let { userMarkers[user.userId] = it }
                }

                // Update polyline track
                trackingService?.getUserTracks()?.get(user.userId)?.let { track ->
                    if (track.size > 1) {
                        val points = track.map { LatLng(it.first, it.second) }
                        val polyline = userPolylines[user.userId]

                        if (polyline != null) {
                            polyline.points = points
                        } else {
                            val newPolyline = map.addPolyline(
                                PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE)
                                    .width(5f)
                            )
                            userPolylines[user.userId] = newPolyline
                        }
                    }
                }
            }
        }
    }

    private fun clearMapTracks() {
        trackingService?.clearTracks()
        userPolylines.values.forEach { it.remove() }
        userPolylines.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
        releaseWakeLock()
        soundPool.release()
    }
}
