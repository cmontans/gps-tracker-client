package com.tracker.gps.wear

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.tracker.gps.shared.model.TrackingState
import com.tracker.gps.shared.model.UserData
import com.tracker.gps.wear.service.WearLocationService
import com.tracker.gps.wear.theme.WearAppTheme

class MainActivity : ComponentActivity() {
    private var locationService: WearLocationService? = null
    private var serviceBound = false

    private val trackingState = mutableStateOf(TrackingState(isTracking = false))
    private val users = mutableStateOf<List<UserData>>(emptyList())
    private val isConnected = mutableStateOf(false)
    private val hasGps = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WearLocationService.LocalBinder
            locationService = binder.getService()
            serviceBound = true

            // Set up listeners
            locationService?.setListener(object : WearLocationService.ServiceListener {
                override fun onTrackingStateChanged(state: TrackingState) {
                    trackingState.value = state
                }

                override fun onUsersUpdate(userList: List<UserData>) {
                    users.value = userList
                }

                override fun onConnectionStatusChanged(connected: Boolean) {
                    isConnected.value = connected
                }

                override fun onGpsStatusChanged(active: Boolean) {
                    hasGps.value = active
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            locationService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, can proceed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("WEAR_APP", "onCreate started")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

        // Bind to service
        Intent(this, WearLocationService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        Log.d("WEAR_APP", "setting content")
        setContent {
            WearApp(
                trackingState = trackingState.value,
                users = users.value,
                isConnected = isConnected.value,
                hasGps = hasGps.value,
                onStartTracking = { userName, groupName ->
                    startTracking(userName, groupName)
                },
                onStopTracking = {
                    stopTracking()
                },
                onGroupHorn = {
                    locationService?.triggerGroupHorn()
                },
                onResetStats = {
                    locationService?.resetStats()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startTracking(userName: String, groupName: String) {
        val intent = Intent(this, WearLocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        locationService?.startTracking(userName, groupName)
    }

    private fun stopTracking() {
        locationService?.stopTracking()
    }

    fun wakeScreen(duration: Long) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "GPSTracker:JumpWake"
            )
            wakeLock.acquire(duration)
        } catch (e: Exception) {
            Log.e("WEAR_APP", "Error waking screen: ${e.message}")
        }
    }

    fun setKeepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun WearApp(
    trackingState: TrackingState,
    users: List<UserData>,
    isConnected: Boolean,
    hasGps: Boolean,
    onStartTracking: (String, String) -> Unit,
    onStopTracking: () -> Unit,
    onGroupHorn: () -> Unit,
    onResetStats: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(trackingState.isCurrentlyJumping) {
        Log.d("WEAR_APP", "isCurrentlyJumping changed: ${trackingState.isCurrentlyJumping}")
        if (trackingState.isCurrentlyJumping) {
            (context as? MainActivity)?.let {
                Log.d("WEAR_APP", "Triggering wakeScreen")
                it.wakeScreen(30000) // Wake for 30s (or until jump ends + 10s)
                it.setKeepScreenOn(true)
            }
        } else {
            // Jump ended, wait 10 seconds before letting screen dim
            Log.d("WEAR_APP", "Jump ended, waiting 10s before dimming")
            delay(10000)
            Log.d("WEAR_APP", "Clearing keepScreenOn")
            (context as? MainActivity)?.setKeepScreenOn(false)
        }
    }
    
    LaunchedEffect(Unit) {
        // Request permissions after the UI has fully drawn to avoid freezing the splash screen
        // (context as? MainActivity)?.checkAndRequestPermissions()
    }

    WearAppTheme {
        val navController = rememberSwipeDismissableNavController()
        val listState = rememberScalingLazyListState()

        Scaffold(
            timeText = {
                TimeText()
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(scalingLazyListState = listState)
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    MainScreen(
                        trackingState = trackingState,
                        isConnected = isConnected,
                        hasGps = hasGps,
                        onStartTracking = onStartTracking,
                        onStopTracking = onStopTracking,
                        onNavigateToUsers = { navController.navigate("users") },
                        onGroupHorn = onGroupHorn,
                        onResetStats = onResetStats
                    )
                }
                composable("users") {
                    UsersScreen(
                        users = users,
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    trackingState: TrackingState,
    isConnected: Boolean,
    hasGps: Boolean,
    onStartTracking: (String, String) -> Unit,
    onStopTracking: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onGroupHorn: () -> Unit,
    onResetStats: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicators
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip(
                    label = if (isConnected) "Connected" else "Offline",
                    color = if (isConnected) Color.Green else Color.Red
                )
                StatusChip(
                    label = if (hasGps) "GPS" else "No GPS",
                    color = if (hasGps) Color.Green else Color.Yellow
                )
            }
        }

        // Speed display
        item {
            SpeedCard(
                label = "Speed",
                speed = trackingState.currentSpeed,
                large = true
            )
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedCard("Max", trackingState.maxSpeed)
                SpeedCard("Avg", trackingState.avgSpeed)
            }
        }

        // Jump Tracker row
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (trackingState.isCurrentlyJumping) "JUMPING!" else "WAITING FOR JUMP",
                    style = MaterialTheme.typography.caption2,
                    color = if (trackingState.isCurrentlyJumping) Color.Green else Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpeedCard("Alt", trackingState.currentAltitude, unit = "m")
                    SpeedCard("Last Jump", trackingState.lastJumpHeight, unit = "m")
                }
            }
        }

        // Start/Stop button
        item {
            Chip(
                onClick = {
                    if (trackingState.isTracking) {
                        onStopTracking()
                    } else {
                        // TODO: Show input dialog for name/group
                        onStartTracking("Watch User", "Default Group")
                    }
                },
                label = {
                    Text(
                        text = if (trackingState.isTracking) "Stop" else "Start",
                        textAlign = TextAlign.Center
                    )
                },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = if (trackingState.isTracking) Color.Red else Color.Green
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Group horn button
        if (trackingState.isTracking) {
            item {
                Chip(
                    onClick = onGroupHorn,
                    label = { Text("Horn") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // View users button
        item {
            Chip(
                onClick = onNavigateToUsers,
                label = { Text("Group") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Reset stats button
        if (trackingState.isTracking) {
            item {
                Chip(
                    onClick = onResetStats,
                    label = { Text("Reset") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun UsersScreen(
    users: List<UserData>,
    listState: ScalingLazyListState
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp)
    ) {
        item {
            Text(
                text = "Group Members",
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (users.isEmpty()) {
            item {
                Text("No members yet", style = MaterialTheme.typography.body2)
            }
        } else {
            items(users.size) { index ->
                val user = users[index]
                Chip(
                    onClick = { },
                    label = {
                        Column {
                            Text(user.userName, style = MaterialTheme.typography.body1)
                            Text(
                                "%.1f km/h".format(user.speed),
                                style = MaterialTheme.typography.caption1
                            )
                        }
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 24.dp)
            .background(color.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = color
        )
    }
}

@Composable
fun SpeedCard(label: String, speed: Double, unit: String = "km/h", large: Boolean = false) {
    Card(
        onClick = { },
        modifier = Modifier
            .padding(4.dp)
            .width(if (large) 150.dp else 70.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(1.dp) // Changed 8dp to 1dp to fix overflow on small screens
        ) {
            Text(
                text = label,
                style = if (large) MaterialTheme.typography.caption1 else MaterialTheme.typography.caption3
            )
            Text(
                text = "%.1f".format(speed),
                style = if (large) MaterialTheme.typography.display1 else MaterialTheme.typography.title3
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.caption3
            )
        }
    }
}
