package com.tracker.gps.wearable

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.tracker.gps.shared.Constants
import com.tracker.gps.shared.model.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneDataLayerService(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "PhoneDataLayer"
    }

    fun sendSpeedUpdate(currentSpeed: Double, maxSpeed: Double, avgSpeed: Double) {
        scope.launch {
            try {
                val putDataReq = PutDataMapRequest.create(Constants.PATH_SPEED_UPDATE).apply {
                    dataMap.putDouble(Constants.KEY_CURRENT_SPEED, currentSpeed)
                    dataMap.putDouble(Constants.KEY_MAX_SPEED, maxSpeed)
                    dataMap.putDouble(Constants.KEY_AVG_SPEED, avgSpeed)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Speed update sent to watch: $currentSpeed km/h")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending speed update", e)
            }
        }
    }

    fun sendUsersUpdate(users: List<UserData>) {
        scope.launch {
            try {
                val usersJson = gson.toJson(users)
                val putDataReq = PutDataMapRequest.create(Constants.PATH_USERS_UPDATE).apply {
                    dataMap.putString(Constants.KEY_USERS_JSON, usersJson)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Users update sent to watch: ${users.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending users update", e)
            }
        }
    }

    fun sendConnectionStatus(connected: Boolean, gpsActive: Boolean) {
        scope.launch {
            try {
                val putDataReq = PutDataMapRequest.create(Constants.PATH_CONNECTION_STATUS).apply {
                    dataMap.putBoolean(Constants.KEY_CONNECTED, connected)
                    dataMap.putBoolean(Constants.KEY_GPS_ACTIVE, gpsActive)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Connection status sent to watch: connected=$connected, gps=$gpsActive")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending connection status", e)
            }
        }
    }

    fun sendTrackingState(active: Boolean) {
        scope.launch {
            try {
                val putDataReq = PutDataMapRequest.create(Constants.PATH_TRACKING_STATE).apply {
                    dataMap.putBoolean(Constants.KEY_TRACKING_ACTIVE, active)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Tracking state sent to watch: $active")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending tracking state", e)
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
