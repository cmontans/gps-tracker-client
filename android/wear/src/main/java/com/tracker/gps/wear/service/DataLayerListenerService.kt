package com.tracker.gps.wear.service

import android.content.Intent
import com.google.android.gms.wearable.*
import com.tracker.gps.shared.model.WearPaths
import com.tracker.gps.shared.util.DataSerializer

/**
 * Service that listens for data changes from the phone app
 */
class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                when (item.uri.path) {
                    WearPaths.TRACKING_STATE -> {
                        // Handle tracking state update from phone
                        DataMapItem.fromDataItem(item).dataMap.also { dataMap ->
                            // Notify local service or UI
                        }
                    }
                    WearPaths.USERS_UPDATE -> {
                        // Handle users list update from phone
                        DataMapItem.fromDataItem(item).dataMap.also { dataMap ->
                            // Notify local service or UI
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearPaths.TRACKING_STATE -> {
                // Broadcast to local components
                val intent = Intent(ACTION_TRACKING_STATE_UPDATE).apply {
                    putExtra(EXTRA_DATA, messageEvent.data)
                }
                sendBroadcast(intent)
            }
            WearPaths.USERS_UPDATE -> {
                val intent = Intent(ACTION_USERS_UPDATE).apply {
                    putExtra(EXTRA_DATA, messageEvent.data)
                }
                sendBroadcast(intent)
            }
            WearPaths.CONNECTION_STATUS -> {
                val intent = Intent(ACTION_CONNECTION_STATUS).apply {
                    putExtra(EXTRA_DATA, messageEvent.data)
                }
                sendBroadcast(intent)
            }
            WearPaths.GROUP_HORN -> {
                val intent = Intent(ACTION_GROUP_HORN)
                sendBroadcast(intent)
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        // Phone connection status changed
        val hasPhone = capabilityInfo.nodes.isNotEmpty()
        val intent = Intent(ACTION_PHONE_CONNECTION_CHANGED).apply {
            putExtra(EXTRA_HAS_PHONE, hasPhone)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_TRACKING_STATE_UPDATE = "com.tracker.gps.wear.TRACKING_STATE_UPDATE"
        const val ACTION_USERS_UPDATE = "com.tracker.gps.wear.USERS_UPDATE"
        const val ACTION_CONNECTION_STATUS = "com.tracker.gps.wear.CONNECTION_STATUS"
        const val ACTION_GROUP_HORN = "com.tracker.gps.wear.GROUP_HORN"
        const val ACTION_PHONE_CONNECTION_CHANGED = "com.tracker.gps.wear.PHONE_CONNECTION_CHANGED"
        const val EXTRA_DATA = "data"
        const val EXTRA_HAS_PHONE = "has_phone"
    }
}
