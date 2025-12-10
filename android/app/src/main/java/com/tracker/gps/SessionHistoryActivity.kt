package com.tracker.gps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.shared.api.SessionApiService
import com.tracker.gps.shared.model.TrackSession
import kotlinx.coroutines.launch

class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var rvSessions: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionsAdapter: SessionHistoryAdapter

    private val sessions = mutableListOf<TrackSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_history)

        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.session_history_title)

        // Initialize views
        rvSessions = findViewById(R.id.rvSessions)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)

        // Set up RecyclerView
        sessionsAdapter = SessionHistoryAdapter(sessions)
        rvSessions.layoutManager = LinearLayoutManager(this)
        rvSessions.adapter = sessionsAdapter

        // Load sessions
        loadSessions()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadSessions() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvSessions.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("user_id", "") ?: ""
                val serverUrl = prefs.getString(
                    getString(R.string.pref_server_url_key),
                    getString(R.string.default_server_url)
                ) ?: getString(R.string.default_server_url)

                if (userId.isEmpty()) {
                    Toast.makeText(
                        this@SessionHistoryActivity,
                        getString(R.string.error_no_user_id),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                // Convert WebSocket URL to HTTP URL
                val httpUrl = serverUrl
                    .replace("wss://", "https://")
                    .replace("ws://", "http://")

                val apiService = SessionApiService(httpUrl)
                val result = apiService.getSessionHistory(userId, limit = 100)

                result.onSuccess { sessionList ->
                    sessions.clear()
                    sessions.addAll(sessionList)
                    sessionsAdapter.notifyDataSetChanged()

                    progressBar.visibility = View.GONE
                    if (sessions.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvSessions.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvSessions.visibility = View.VISIBLE
                    }
                }.onFailure { error ->
                    progressBar.visibility = View.GONE
                    tvEmpty.text = getString(R.string.error_loading_sessions, error.message)
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(
                        this@SessionHistoryActivity,
                        getString(R.string.error_loading_sessions, error.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.text = getString(R.string.error_loading_sessions, e.message)
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(
                    this@SessionHistoryActivity,
                    getString(R.string.error_loading_sessions, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
