package com.tracker.gps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tracker.gps.api.WaypointsApi
import com.tracker.gps.model.Waypoint
import com.tracker.gps.shared.util.Constants

class WaypointsActivity : AppCompatActivity() {

    private lateinit var rvWaypoints: RecyclerView
    private lateinit var tvNoWaypoints: TextView
    private lateinit var tvGroupLabel: TextView
    private lateinit var adapter: WaypointsAdapter
    private lateinit var waypointsApi: WaypointsApi
    
    private var groupName: String = "default"
    private var serverUrl: String = Constants.DEFAULT_SERVER_URL
    private var userName: String = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waypoints)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load preferences
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        groupName = prefs.getString(Constants.PREF_GROUP_NAME, "default") ?: "default"
        serverUrl = prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL) ?: Constants.DEFAULT_SERVER_URL
        userName = prefs.getString(Constants.PREF_USER_NAME, "User") ?: "User"

        waypointsApi = WaypointsApi(serverUrl)

        tvGroupLabel = findViewById(R.id.tvGroupLabel)
        tvGroupLabel.text = "Group: $groupName"
        
        tvNoWaypoints = findViewById(R.id.tvNoWaypoints)
        rvWaypoints = findViewById(R.id.rvWaypoints)
        rvWaypoints.layoutManager = LinearLayoutManager(this)
        
        adapter = WaypointsAdapter(
            onGoTo = { waypoint ->
                // Return to main and show on map if needed
                // For now just show coordinates
                Toast.makeText(this, "Going to: ${waypoint.name}", Toast.LENGTH_SHORT).show()
            },
            onDelete = { waypoint ->
                showDeleteConfirmation(waypoint)
            }
        )
        rvWaypoints.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddWaypoint).setOnClickListener {
            showAddWaypointDialog()
        }

        loadWaypoints()
    }

    private fun loadWaypoints() {
        waypointsApi.getWaypoints(groupName, { waypoints ->
            runOnUiThread {
                adapter.updateWaypoints(waypoints)
                tvNoWaypoints.visibility = if (waypoints.isEmpty()) View.VISIBLE else View.GONE
            }
        }, { error ->
            runOnUiThread {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddWaypointDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_add_waypoint, null)
        
        val etName = dialogView.findViewById<EditText>(R.id.etWaypointName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etWaypointDesc)
        val etLat = dialogView.findViewById<EditText>(R.id.etWaypointLat)
        val etLon = dialogView.findViewById<EditText>(R.id.etWaypointLon)

        // Pre-fill coordinates if possible (hardcoded placeholder for now)
        // In a real app we'd get current location or pick from map
        etLat.setText("0.0")
        etLon.setText("0.0")

        builder.setView(dialogView)
            .setTitle(R.string.add_waypoint)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString()
                val desc = etDesc.text.toString()
                val lat = etLat.text.toString().toDoubleOrNull() ?: 0.0
                val lon = etLon.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    val waypoint = Waypoint(
                        groupName = groupName,
                        name = name,
                        description = desc,
                        latitude = lat,
                        longitude = lon,
                        createdBy = userName
                    )
                    createNewWaypoint(waypoint)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createNewWaypoint(waypoint: Waypoint) {
        waypointsApi.createWaypoint(waypoint, {
            runOnUiThread {
                loadWaypoints()
                Toast.makeText(this, "Waypoint created", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            runOnUiThread {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteConfirmation(waypoint: Waypoint) {
        AlertDialog.Builder(this)
            .setTitle("Delete Waypoint?")
            .setMessage("Are you sure you want to delete ${waypoint.name}?")
            .setPositiveButton("Delete") { _, _ ->
                waypoint.id?.let { id ->
                    waypointsApi.deleteWaypoint(id, {
                        runOnUiThread {
                            loadWaypoints()
                            Toast.makeText(this, "Waypoint deleted", Toast.LENGTH_SHORT).show()
                        }
                    }, { error ->
                        runOnUiThread {
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
