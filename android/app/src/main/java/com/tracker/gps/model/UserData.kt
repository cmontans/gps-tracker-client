package com.tracker.gps.model

data class UserData(
    val userId: String,
    val userName: String,
    val speed: Double,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
