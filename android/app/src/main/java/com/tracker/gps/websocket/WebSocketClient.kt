package com.tracker.gps.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tracker.gps.shared.model.UserData
import com.tracker.gps.shared.model.UsersUpdateMessage
import com.tracker.gps.shared.model.WebSocketMessage
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class GPSWebSocketClient(
    serverUri: URI,
    private val listener: WebSocketListener
) : WebSocketClient(serverUri) {

    private val gson = Gson()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private var isManualClose = false

    interface WebSocketListener {
        fun onConnected()
        fun onDisconnected()
        fun onUsersUpdate(users: List<UserData>)
        fun onGroupHorn()
        fun onError(error: String)
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d(TAG, "WebSocket connected")
        reconnectAttempts = 0
        listener.onConnected()
    }

    override fun onMessage(message: String?) {
        message?.let {
            try {
                Log.d(TAG, "Received: $it")
                val jsonObject = gson.fromJson(it, JsonObject::class.java)
                val type = jsonObject.get("type")?.asString

                when (type) {
                    "users" -> {
                        val usersMessage = gson.fromJson(it, UsersUpdateMessage::class.java)
                        listener.onUsersUpdate(usersMessage.users)
                    }
                    "group-horn" -> {
                        listener.onGroupHorn()
                    }
                    "ping" -> {
                        sendPong()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message", e)
                listener.onError("Error parsing message: ${e.message}")
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "WebSocket closed: $reason")
        listener.onDisconnected()

        if (!isManualClose && reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delay = Math.min(1000L * (1 shl reconnectAttempts), 30000L)
            Log.d(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts)")

            Thread {
                Thread.sleep(delay)
                try {
                    reconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Reconnection failed", e)
                }
            }.start()
        }
    }

    override fun onError(ex: Exception?) {
        Log.e(TAG, "WebSocket error", ex)
        listener.onError("WebSocket error: ${ex?.message}")
    }

    fun sendRegister(userId: String, userName: String, groupName: String) {
        val message = WebSocketMessage.Register(
            userId = userId,
            userName = userName,
            groupName = groupName
        )
        send(gson.toJson(message))
        Log.d(TAG, "Sent register: $userId, $userName, $groupName")
    }

    fun sendSpeed(userId: String, speed: Double, latitude: Double, longitude: Double, bearing: Float) {
        val message = WebSocketMessage.Speed(
            userId = userId,
            speed = speed,
            latitude = latitude,
            longitude = longitude,
            bearing = bearing
        )
        send(gson.toJson(message))
    }

    fun sendGroupHorn(userId: String) {
        val message = WebSocketMessage.GroupHorn(userId = userId)
        send(gson.toJson(message))
        Log.d(TAG, "Sent group horn")
    }

    private fun sendPong() {
        val message = WebSocketMessage.Pong()
        send(gson.toJson(message))
    }

    fun closeManually() {
        isManualClose = true
        close()
    }

    companion object {
        private const val TAG = "GPSWebSocketClient"
    }
}
