package com.tracker.gps.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import com.tracker.gps.shared.model.*
import com.tracker.gps.shared.util.Constants
import com.tracker.gps.shared.util.DataSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Manages communication between phone and smartwatch via Wearable Data Layer
 */
class WearableDataService(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context)

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var connectedNodes = mutableSetOf<String>()

    interface WearableListener {
        fun onWearControlCommand(command: WearControlCommand)
        fun onWearLocationUpdate(userId: String, location: Map<String, Any>)
    }

    private var listener: WearableListener? = null

    fun setListener(listener: WearableListener) {
        this.listener = listener
    }

    init {
        setupMessageListener()
        updateConnectedNodes()
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            serviceScope.launch {
                handleMessageReceived(messageEvent)
            }
        }

        // Listen for capability changes (watch connection/disconnection)
        capabilityClient.addListener(
            { capabilityInfo ->
                updateConnectedNodesFromCapability(capabilityInfo)
            },
            Constants.CAPABILITY_TRACKER_APP
        )
    }

    private suspend fun handleMessageReceived(messageEvent: MessageEvent) {
        try {
            when (messageEvent.path) {
                WearPaths.CONTROL_COMMAND -> {
                    val command = DataSerializer.fromBytes<WearControlCommand>(messageEvent.data)
                    listener?.onWearControlCommand(command)
                }
                WearPaths.LOCATION_UPDATE -> {
                    val data = DataSerializer.fromBytes<Map<String, Any>>(messageEvent.data)
                    val userId = data["userId"] as? String ?: return
                    listener?.onWearLocationUpdate(userId, data)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${messageEvent.path}", e)
        }
    }

    private fun updateConnectedNodesFromCapability(capabilityInfo: CapabilityInfo) {
        connectedNodes.clear()
        connectedNodes.addAll(capabilityInfo.nodes.map { it.id })
        Log.d(TAG, "Connected nodes updated: ${connectedNodes.size} watches")
    }

    private fun updateConnectedNodes() {
        serviceScope.launch {
            try {
                val capability = capabilityClient.getCapability(
                    Constants.CAPABILITY_TRACKER_APP,
                    CapabilityClient.FILTER_REACHABLE
                ).await()

                connectedNodes.clear()
                connectedNodes.addAll(capability.nodes.map { it.id })
                Log.d(TAG, "Found ${connectedNodes.size} connected watches")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting connected nodes", e)
            }
        }
    }

    /**
     * Send tracking state to all connected watches
     */
    fun sendTrackingState(state: TrackingState) {
        serviceScope.launch {
            val data = DataSerializer.toBytes(state)
            connectedNodes.forEach { nodeId ->
                try {
                    messageClient.sendMessage(nodeId, WearPaths.TRACKING_STATE, data).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending tracking state to $nodeId", e)
                }
            }
        }
    }

    /**
     * Send users list to all connected watches
     */
    fun sendUsersUpdate(users: List<UserData>) {
        serviceScope.launch {
            val data = DataSerializer.toBytes(users)
            connectedNodes.forEach { nodeId ->
                try {
                    messageClient.sendMessage(nodeId, WearPaths.USERS_UPDATE, data).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending users update to $nodeId", e)
                }
            }
        }
    }

    /**
     * Send connection status to all connected watches
     */
    fun sendConnectionStatus(status: ConnectionStatus) {
        serviceScope.launch {
            val data = DataSerializer.toBytes(status)
            connectedNodes.forEach { nodeId ->
                try {
                    messageClient.sendMessage(nodeId, WearPaths.CONNECTION_STATUS, data).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending connection status to $nodeId", e)
                }
            }
        }
    }

    /**
     * Send group horn notification to all connected watches
     */
    fun sendGroupHorn() {
        serviceScope.launch {
            connectedNodes.forEach { nodeId ->
                try {
                    messageClient.sendMessage(nodeId, WearPaths.GROUP_HORN, ByteArray(0)).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending group horn to $nodeId", e)
                }
            }
        }
    }

    /**
     * Check if any watches are connected
     */
    fun hasConnectedWatches(): Boolean {
        return connectedNodes.isNotEmpty()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        serviceScope.cancel()
        messageClient.removeListener { }
    }

    companion object {
        private const val TAG = "WearableDataService"
    }
}
