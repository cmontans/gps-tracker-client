package com.tracker.gps.service

import android.content.Intent
import com.google.android.gms.wearable.*
import com.tracker.gps.shared.model.WearControlCommand
import com.tracker.gps.shared.model.WearPaths
import com.tracker.gps.shared.util.DataSerializer

/**
 * Service that listens for data and messages from connected watches
 */
class PhoneDataLayerListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearPaths.CONTROL_COMMAND -> {
                try {
                    val command = DataSerializer.fromBytes<WearControlCommand>(messageEvent.data)
                    handleControlCommand(command)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error parsing control command", e)
                }
            }
            WearPaths.LOCATION_UPDATE -> {
                // Forward to tracking service
                val intent = Intent(ACTION_WEAR_LOCATION_UPDATE).apply {
                    putExtra(EXTRA_NODE_ID, messageEvent.sourceNodeId)
                    putExtra(EXTRA_DATA, messageEvent.data)
                }
                sendBroadcast(intent)
            }
        }
    }

    private fun handleControlCommand(command: WearControlCommand) {
        when (command) {
            is WearControlCommand.StartTracking -> {
                val intent = Intent(ACTION_WEAR_START_TRACKING).apply {
                    putExtra(EXTRA_USER_NAME, command.userName)
                    putExtra(EXTRA_GROUP_NAME, command.groupName)
                }
                sendBroadcast(intent)
            }
            is WearControlCommand.StopTracking -> {
                sendBroadcast(Intent(ACTION_WEAR_STOP_TRACKING))
            }
            is WearControlCommand.TriggerGroupHorn -> {
                sendBroadcast(Intent(ACTION_WEAR_GROUP_HORN))
            }
            is WearControlCommand.ResetStats -> {
                sendBroadcast(Intent(ACTION_WEAR_RESET_STATS))
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        // Notify that watch connection status changed
        val hasWatches = capabilityInfo.nodes.isNotEmpty()
        val intent = Intent(ACTION_WATCH_CONNECTION_CHANGED).apply {
            putExtra(EXTRA_HAS_WATCHES, hasWatches)
            putExtra(EXTRA_NODE_COUNT, capabilityInfo.nodes.size)
        }
        sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "PhoneDataLayerListener"

        const val ACTION_WEAR_START_TRACKING = "com.tracker.gps.WEAR_START_TRACKING"
        const val ACTION_WEAR_STOP_TRACKING = "com.tracker.gps.WEAR_STOP_TRACKING"
        const val ACTION_WEAR_GROUP_HORN = "com.tracker.gps.WEAR_GROUP_HORN"
        const val ACTION_WEAR_RESET_STATS = "com.tracker.gps.WEAR_RESET_STATS"
        const val ACTION_WEAR_LOCATION_UPDATE = "com.tracker.gps.WEAR_LOCATION_UPDATE"
        const val ACTION_WATCH_CONNECTION_CHANGED = "com.tracker.gps.WATCH_CONNECTION_CHANGED"

        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_NODE_ID = "node_id"
        const val EXTRA_DATA = "data"
        const val EXTRA_HAS_WATCHES = "has_watches"
        const val EXTRA_NODE_COUNT = "node_count"
    }
}
