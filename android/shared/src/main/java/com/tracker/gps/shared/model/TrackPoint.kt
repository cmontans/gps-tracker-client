package com.tracker.gps.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single GPS point in a tracking session
 */
@Entity(tableName = "track_points")
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val bearing: Float,
    val accuracy: Float,
    val altitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
