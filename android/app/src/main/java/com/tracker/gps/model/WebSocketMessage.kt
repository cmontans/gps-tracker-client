package com.tracker.gps.model

import com.google.gson.JsonObject

sealed class WebSocketMessage {
    data class Register(
        val type: String = "register",
        val userId: String,
        val userName: String,
        val groupName: String
    ) : WebSocketMessage()

    data class Speed(
        val type: String = "speed",
        val userId: String,
        val speed: Double,
        val latitude: Double,
        val longitude: Double,
        val bearing: Float
    ) : WebSocketMessage()

    data class GroupHorn(
        val type: String = "group-horn",
        val userId: String
    ) : WebSocketMessage()

    data class Ping(
        val type: String = "ping"
    ) : WebSocketMessage()

    data class Pong(
        val type: String = "pong"
    ) : WebSocketMessage()
}

data class UsersUpdateMessage(
    val type: String,
    val users: List<UserData>
)
