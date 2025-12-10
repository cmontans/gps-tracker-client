package com.tracker.gps.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserData(
    @PrimaryKey
    val userId: String,
    val userName: String,
    val speed: Double,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val groupName: String = ""
)
