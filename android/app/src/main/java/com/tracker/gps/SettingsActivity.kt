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
    private lateinit var switchVisualizerMode: SwitchMaterial
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        etServerUrl = findViewById(R.id.etServerUrl)
        etUserId = findViewById(R.id.etUserId)
        switchAutoCenter = findViewById(R.id.switchAutoCenter)
        switchVisualizerMode = findViewById(R.id.switchVisualizerMode)
        btnSave = findViewById(R.id.btnSave)

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
        val visualizerMode = prefs.getBoolean(getString(R.string.pref_visualizer_mode_key), false)

        etServerUrl.setText(serverUrl)
        etUserId.setText(userId)
        switchAutoCenter.isChecked = autoCenter
        switchVisualizerMode.isChecked = visualizerMode
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(getString(R.string.pref_server_url_key), etServerUrl.text.toString())
            putBoolean(getString(R.string.pref_auto_center_key), switchAutoCenter.isChecked)
            putBoolean(getString(R.string.pref_visualizer_mode_key), switchVisualizerMode.isChecked)
            apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val PREFS_NAME = "gps_tracker_prefs"
    }
}
