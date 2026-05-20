package com.tracker.gps.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jump_sessions")
data class JumpSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val maxHeight: Double,
    val hangtime: Long // In milliseconds
)
