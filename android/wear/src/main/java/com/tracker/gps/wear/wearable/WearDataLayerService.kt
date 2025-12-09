package com.tracker.gps.wear.wearable

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tracker.gps.shared.Constants
import com.tracker.gps.shared.model.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearDataLayerService(private val context: Context) {
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "WearDataLayer"
    }

    fun sendStartTrackingMessage(userId: String, userName: String, groupName: String) {
        scope.launch {
            try {
                val data = "$userId|$userName|$groupName"
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(
                        node.id,
                        Constants.PATH_START_TRACKING,
                        data.toByteArray()
                    ).await()
                    Log.d(TAG, "Start tracking message sent to phone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending start tracking message", e)
            }
        }
    }

    fun sendStopTrackingMessage() {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(
                        node.id,
                        Constants.PATH_STOP_TRACKING,
                        ByteArray(0)
                    ).await()
                    Log.d(TAG, "Stop tracking message sent to phone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending stop tracking message", e)
            }
        }
    }

    fun sendResetStatsMessage() {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(
                        node.id,
                        Constants.PATH_RESET_STATS,
                        ByteArray(0)
                    ).await()
                    Log.d(TAG, "Reset stats message sent to phone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending reset stats message", e)
            }
        }
    }

    fun sendGroupHornMessage() {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()

                for (node in nodes) {
                    messageClient.sendMessage(
                        node.id,
                        Constants.PATH_GROUP_HORN,
                        ByteArray(0)
                    ).await()
                    Log.d(TAG, "Group horn message sent to phone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending group horn message", e)
            }
        }
    }

    suspend fun getConnectedNodes(): List<Node> {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected nodes", e)
            emptyList()
        }
    }
}
