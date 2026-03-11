package com.tracker.gps.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.tracker.gps.db.AppDatabase
import com.tracker.gps.model.JumpSession
import com.tracker.gps.service.JumpTrackingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.tracker.gps.util.JumpSensitivity

class JumpViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val jumpDao = db.jumpDao()

    private var jumpService: JumpTrackingService? = null
    private val _isServiceBound = MutableStateFlow(false)

    private val prefs = application.getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)

    private val _currentSensitivity = MutableStateFlow(
        try {
            JumpSensitivity.valueOf(prefs.getString("jump_sensitivity", "MEDIA") ?: "MEDIA")
        } catch (e: Exception) {
            JumpSensitivity.MEDIA
        }
    )
    val currentSensitivity = _currentSensitivity.asStateFlow()

    val isTracking = _isServiceBound.flatMapLatest { bound ->
        if (bound) jumpService!!.isTracking else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCurrentlyJumping = _isServiceBound.flatMapLatest { bound ->
        if (bound) jumpService!!.isCurrentlyJumping else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentAltitude = _isServiceBound.flatMapLatest { bound ->
        if (bound) jumpService!!.currentAltitude else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lastJumpHeight = _isServiceBound.flatMapLatest { bound ->
        if (bound) jumpService!!.lastJumpHeight else flowOf(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val jumpHistory: StateFlow<List<JumpSession>> = jumpDao.getAllJumps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as JumpTrackingService.LocalBinder
            jumpService = binder.getService()
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            jumpService = null
            _isServiceBound.value = false
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, JumpTrackingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (_isServiceBound.value) {
            context.unbindService(serviceConnection)
            _isServiceBound.value = false
        }
    }

    fun toggleTracking(context: Context) {
        val intent = Intent(context, JumpTrackingService::class.java)
        if (isTracking.value) {
            intent.action = JumpTrackingService.ACTION_STOP_TRACKING
        } else {
            intent.action = JumpTrackingService.ACTION_START_TRACKING
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun setSensitivity(sensitivity: JumpSensitivity) {
        prefs.edit().putString("jump_sensitivity", sensitivity.name).apply()
        _currentSensitivity.value = sensitivity
        jumpService?.setSensitivity(sensitivity)
    }

    fun deleteJump(jump: JumpSession) {
        viewModelScope.launch {
            jumpDao.deleteJump(jump)
        }
    }
}
