package com.tracker.gps.shared.model

/**
 * WebSocket message types for communication with the server
 */
sealed class WebSocketMessage {
    data class Register(
        val type: String = "register",
        val userId: String,
        val userName: String,
        val groupName: String
    )

    data class Speed(
        val type: String = "speed",
        val userId: String,
        val speed: Double,
        val latitude: Double,
        val longitude: Double,
        val bearing: Float
    )

    data class GroupHorn(
        val type: String = "group-horn",
        val userId: String
    )

    data class Ping(
        val type: String = "ping"
    )

    data class Pong(
        val type: String = "pong"
    )
}

/**
 * Server response with list of users in the group
 */
data class UsersUpdateMessage(
    val type: String,
    val users: List<UserData>
)
