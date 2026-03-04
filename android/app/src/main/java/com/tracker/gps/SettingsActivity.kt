package com.tracker.gps

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.Button

class SettingsActivity : AppCompatActivity() {

    private lateinit var etServerUrl: TextInputEditText
    private lateinit var etUserId: TextInputEditText
    private lateinit var switchAutoCenter: SwitchMaterial
    private lateinit var switchVoiceEnabled: SwitchMaterial
    private lateinit var switchVisualizerMode: SwitchMaterial
    private lateinit var etMinSpeed: TextInputEditText
    private lateinit var spinnerSpeedUnit: android.widget.AutoCompleteTextView
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        etServerUrl = findViewById(R.id.etServerUrl)
        etUserId = findViewById(R.id.etUserId)
        switchAutoCenter = findViewById(R.id.switchAutoCenter)
        switchVoiceEnabled = findViewById(R.id.switchVoiceEnabled)
        switchVisualizerMode = findViewById(R.id.switchVisualizerMode)
        etMinSpeed = findViewById(R.id.etMinSpeed)
        spinnerSpeedUnit = findViewById(R.id.spinnerSpeedUnit)
        btnSave = findViewById(R.id.btnSave)

        setupSpeedUnitSpinner()
        loadSettings()

        btnSave.setOnClickListener {
            saveSettings()
            finish()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serverUrl = prefs.getString(
            getString(R.string.pref_server_url_key),
            getString(R.string.default_server_url)
        )
        val userId = prefs.getString(getString(R.string.pref_user_id_key), "")
        val autoCenter = prefs.getBoolean(getString(R.string.pref_auto_center_key), true)
        val voiceEnabled = prefs.getBoolean(getString(R.string.pref_voice_enabled_key), false)
        val visualizerMode = prefs.getBoolean(getString(R.string.pref_visualizer_mode_key), false)
        val minSpeed = prefs.getFloat(getString(R.string.pref_voice_min_speed_key), 22f)

        etServerUrl.setText(serverUrl)
        etUserId.setText(userId)
        switchAutoCenter.isChecked = autoCenter
        switchVoiceEnabled.isChecked = voiceEnabled
        switchVisualizerMode.isChecked = visualizerMode
        etMinSpeed.setText(minSpeed.toString())

        val speedUnit = prefs.getString(getString(R.string.pref_speed_unit_key), "kmh")
        val unitLabel = when(speedUnit) {
            "mph" -> getString(R.string.unit_mph)
            "knots" -> getString(R.string.unit_knots)
            else -> getString(R.string.unit_kmh)
        }
        spinnerSpeedUnit.setText(unitLabel, false)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(getString(R.string.pref_server_url_key), etServerUrl.text.toString())
            putBoolean(getString(R.string.pref_auto_center_key), switchAutoCenter.isChecked)
            putBoolean(getString(R.string.pref_voice_enabled_key), switchVoiceEnabled.isChecked)
            putBoolean(getString(R.string.pref_visualizer_mode_key), switchVisualizerMode.isChecked)
            val minSpeed = etMinSpeed.text.toString().toFloatOrNull() ?: 22f
            putFloat(getString(R.string.pref_voice_min_speed_key), minSpeed)

            val unitText = spinnerSpeedUnit.text.toString()
            val unitValue = when(unitText) {
                getString(R.string.unit_mph) -> "mph"
                getString(R.string.unit_knots) -> "knots"
                else -> "kmh"
            }
            putString(getString(R.string.pref_speed_unit_key), unitValue)
            apply()
        }
    }

    private fun setupSpeedUnitSpinner() {
        val units = arrayOf(
            getString(R.string.unit_kmh),
            getString(R.string.unit_mph),
            getString(R.string.unit_knots)
        )
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        spinnerSpeedUnit.setAdapter(adapter)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val PREFS_NAME = "gps_tracker_prefs"
    }
}
