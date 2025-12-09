package com.tracker.gps.wearable

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tracker.gps.MainActivity
import com.tracker.gps.shared.Constants

class PhoneMessageListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "PhoneMessageListener"
        const val ACTION_START_TRACKING = "com.tracker.gps.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.tracker.gps.STOP_TRACKING"
        const val ACTION_RESET_STATS = "com.tracker.gps.RESET_STATS"
        const val ACTION_GROUP_HORN = "com.tracker.gps.GROUP_HORN"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: ${messageEvent.path}")

        when (messageEvent.path) {
            Constants.PATH_START_TRACKING -> {
                val data = String(messageEvent.data)
                Log.d(TAG, "Start tracking requested from watch: $data")
                sendBroadcast(Intent(ACTION_START_TRACKING).apply {
                    putExtra("data", data)
                })
            }
            Constants.PATH_STOP_TRACKING -> {
                Log.d(TAG, "Stop tracking requested from watch")
                sendBroadcast(Intent(ACTION_STOP_TRACKING))
            }
            Constants.PATH_RESET_STATS -> {
                Log.d(TAG, "Reset stats requested from watch")
                sendBroadcast(Intent(ACTION_RESET_STATS))
            }
            Constants.PATH_GROUP_HORN -> {
                Log.d(TAG, "Group horn requested from watch")
                sendBroadcast(Intent(ACTION_GROUP_HORN))
            }
        }
    }
}
