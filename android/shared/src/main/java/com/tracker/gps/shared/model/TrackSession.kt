package com.tracker.gps.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a tracking session with metadata
 */
@Entity(tableName = "track_sessions")
data class TrackSession(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val userName: String,
    val groupName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val maxSpeed: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val totalDistance: Double = 0.0,
    val duration: Long = 0,
    val pointCount: Int = 0
)
