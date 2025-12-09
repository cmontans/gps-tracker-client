package com.tracker.gps.wear.wearable

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tracker.gps.shared.Constants
import com.tracker.gps.shared.model.UserData

class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataListener"
        const val ACTION_SPEED_UPDATE = "com.tracker.gps.wear.SPEED_UPDATE"
        const val ACTION_USERS_UPDATE = "com.tracker.gps.wear.USERS_UPDATE"
        const val ACTION_CONNECTION_STATUS = "com.tracker.gps.wear.CONNECTION_STATUS"
        const val ACTION_TRACKING_STATE = "com.tracker.gps.wear.TRACKING_STATE"
    }

    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                when (item.uri.path) {
                    Constants.PATH_SPEED_UPDATE -> {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val currentSpeed = dataMap.getDouble(Constants.KEY_CURRENT_SPEED)
                        val maxSpeed = dataMap.getDouble(Constants.KEY_MAX_SPEED)
                        val avgSpeed = dataMap.getDouble(Constants.KEY_AVG_SPEED)

                        Log.d(TAG, "Speed update received: current=$currentSpeed, max=$maxSpeed, avg=$avgSpeed")

                        val intent = Intent(ACTION_SPEED_UPDATE).apply {
                            putExtra(Constants.KEY_CURRENT_SPEED, currentSpeed)
                            putExtra(Constants.KEY_MAX_SPEED, maxSpeed)
                            putExtra(Constants.KEY_AVG_SPEED, avgSpeed)
                        }
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    Constants.PATH_USERS_UPDATE -> {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val usersJson = dataMap.getString(Constants.KEY_USERS_JSON)

                        Log.d(TAG, "Users update received")

                        val intent = Intent(ACTION_USERS_UPDATE).apply {
                            putExtra(Constants.KEY_USERS_JSON, usersJson)
                        }
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    Constants.PATH_CONNECTION_STATUS -> {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val connected = dataMap.getBoolean(Constants.KEY_CONNECTED)
                        val gpsActive = dataMap.getBoolean(Constants.KEY_GPS_ACTIVE)

                        Log.d(TAG, "Connection status received: connected=$connected, gps=$gpsActive")

                        val intent = Intent(ACTION_CONNECTION_STATUS).apply {
                            putExtra(Constants.KEY_CONNECTED, connected)
                            putExtra(Constants.KEY_GPS_ACTIVE, gpsActive)
                        }
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }

                    Constants.PATH_TRACKING_STATE -> {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val trackingActive = dataMap.getBoolean(Constants.KEY_TRACKING_ACTIVE)

                        Log.d(TAG, "Tracking state received: $trackingActive")

                        val intent = Intent(ACTION_TRACKING_STATE).apply {
                            putExtra(Constants.KEY_TRACKING_ACTIVE, trackingActive)
                        }
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }
                }
            }
        }
    }
}
