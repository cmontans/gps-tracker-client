package com.tracker.gps

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.api.SpeedHistoryApi
import com.tracker.gps.api.SpeedStatistics

class HistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvTotalRecords: TextView
    private lateinit var tvHighestSpeed: TextView
    private lateinit var tvAverageMax: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyState: View
    private lateinit var tvEmptyState: TextView

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var speedHistoryApi: SpeedHistoryApi

    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Get user ID from SharedPreferences
        val prefs = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
        userId = prefs.getString("userId", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, R.string.error_user_id_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Get server URL
        val serverUrl = prefs.getString(getString(R.string.pref_server_url_key), getString(R.string.default_server_url)) ?: getString(R.string.default_server_url)
        speedHistoryApi = SpeedHistoryApi(serverUrl)

        setupViews()
        loadData()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvTotalRecords = findViewById(R.id.tvTotalRecords)
        tvHighestSpeed = findViewById(R.id.tvHighestSpeed)
        tvAverageMax = findViewById(R.id.tvAverageMax)
        rvHistory = findViewById(R.id.rvHistory)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        historyAdapter = HistoryAdapter()
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter
    }

    private fun loadData() {
        tvEmptyState.text = getString(R.string.loading)
        layoutEmptyState.visibility = View.VISIBLE
        rvHistory.visibility = View.GONE

        // Load statistics
        speedHistoryApi.getSpeedStatistics(
            userId = userId,
            onSuccess = { stats ->
                runOnUiThread {
                    updateStatistics(stats)
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error loading statistics: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Load history records
        speedHistoryApi.getSpeedHistory(
            userId = userId,
            limit = 100,
            offset = 0,
            onSuccess = { records ->
                runOnUiThread {
                    if (records.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = getString(R.string.no_history_records)
                        rvHistory.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        historyAdapter.updateRecords(records)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    layoutEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Error: $error"
                    rvHistory.visibility = View.GONE
                    Toast.makeText(this, "Error loading history: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun updateStatistics(stats: SpeedStatistics) {
        tvTotalRecords.text = stats.totalRecords
        tvHighestSpeed.text = String.format("%.1f", stats.highestSpeed?.toDoubleOrNull() ?: 0.0)
        tvAverageMax.text = String.format("%.1f", stats.averageMaxSpeed?.toDoubleOrNull() ?: 0.0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
