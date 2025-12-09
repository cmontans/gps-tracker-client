# Garmin Integration for Android App

This guide explains how to enable full Garmin device support in the GPS Tracker Android app.

## Overview

The Android app includes a `GarminCommService` that provides a framework for communicating with Garmin devices via the ConnectIQ Mobile SDK. Currently, this is implemented as a placeholder that can be activated by integrating the official Garmin SDK.

## Current Status

✅ **Completed:**
- Garmin communication service structure
- Integration points in LocationTrackingService
- Message protocol definition
- Android-side data synchronization

⚠️ **Requires SDK Integration:**
- ConnectIQ Mobile SDK dependency
- Garmin Developer account and app registration
- Testing with physical Garmin devices

## Quick Start

### 1. Register as Garmin Developer

1. Go to [Garmin Developer Portal](https://developer.garmin.com/)
2. Create a developer account
3. Accept developer agreements
4. Register your app in the developer dashboard

### 2. Add ConnectIQ Mobile SDK

Add the SDK dependency to `android/app/build.gradle`:

```gradle
dependencies {
    // ... existing dependencies ...

    // ConnectIQ Mobile SDK
    implementation 'com.garmin.connectiq:ciq-companion-app-sdk:2.0.3'
}
```

### 3. Configure App ID

Update `GarminCommService.kt` with your app ID from Garmin Developer Portal:

```kotlin
companion object {
    private const val TAG = "GarminCommService"
    private const val GARMIN_APP_ID = "your-app-id-here" // From Garmin Developer Portal

    // ... rest of companion object ...
}
```

### 4. Implement SDK Initialization

Uncomment and implement the SDK initialization in `GarminCommService.kt`:

```kotlin
private lateinit var connectIQ: ConnectIQ
private var currentDevice: IQDevice? = null
private var currentApp: IQApp? = null

fun initialize() {
    connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS)
    connectIQ.initialize(context, true, object : ConnectIQ.ConnectIQListener {
        override fun onSdkReady() {
            isInitialized = true
            Log.d(TAG, "ConnectIQ SDK ready")
            scanForDevices()
        }

        override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
            Log.e(TAG, "SDK initialization error: $status")
        }

        override fun onSdkShutDown() {
            isInitialized = false
        }
    })
}
```

### 5. Implement Device Discovery

Add device scanning functionality:

```kotlin
private fun scanForDevices() {
    try {
        val devices = connectIQ.connectedDevices
        if (devices.isNotEmpty()) {
            currentDevice = devices[0]
            Log.d(TAG, "Found device: ${currentDevice?.friendlyName}")
            findApp()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error scanning for devices", e)
    }
}

private fun findApp() {
    if (currentDevice == null) return

    try {
        val app = IQApp(GARMIN_APP_ID)
        connectIQ.getApplicationInfo(
            GARMIN_APP_ID,
            currentDevice,
            object : ConnectIQ.IQApplicationInfoListener {
                override fun onApplicationInfoReceived(appInfo: IQApp?) {
                    if (appInfo != null) {
                        currentApp = appInfo
                        isDeviceConnected = true
                        listener?.onGarminDeviceConnected()
                        Log.d(TAG, "GPS Tracker app found on device")
                    }
                }

                override fun onApplicationNotInstalled(appId: String?) {
                    Log.w(TAG, "GPS Tracker app not installed on device")
                }
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error finding app", e)
    }
}
```

### 6. Implement Message Sending

Implement the message sending functionality:

```kotlin
private fun sendMessageToDevice(message: JSONObject) {
    if (currentDevice == null || currentApp == null) {
        Log.w(TAG, "Cannot send message: device or app not available")
        return
    }

    try {
        connectIQ.sendMessage(
            currentDevice,
            currentApp,
            message.toString(),
            object : ConnectIQ.IQSendMessageListener {
                override fun onMessageStatus(
                    device: IQDevice?,
                    app: IQApp?,
                    status: ConnectIQ.IQMessageStatus
                ) {
                    when (status) {
                        ConnectIQ.IQMessageStatus.SUCCESS -> {
                            Log.d(TAG, "Message sent successfully")
                        }
                        ConnectIQ.IQMessageStatus.FAILURE_UNKNOWN -> {
                            Log.e(TAG, "Failed to send message: unknown error")
                        }
                        ConnectIQ.IQMessageStatus.FAILURE_INVALID_DEVICE -> {
                            Log.e(TAG, "Failed to send message: invalid device")
                        }
                        ConnectIQ.IQMessageStatus.FAILURE_DEVICE_NOT_CONNECTED -> {
                            Log.e(TAG, "Failed to send message: device not connected")
                            isDeviceConnected = false
                        }
                        else -> {
                            Log.w(TAG, "Message status: $status")
                        }
                    }
                }
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error sending message", e)
    }
}
```

### 7. Implement Message Receiving

Set up message listener to receive data from Garmin device:

```kotlin
private fun registerMessageListener() {
    if (currentDevice == null || currentApp == null) return

    try {
        connectIQ.registerForAppEvents(
            currentDevice,
            currentApp,
            object : ConnectIQ.IQApplicationEventListener {
                override fun onMessageReceived(
                    device: IQDevice?,
                    app: IQApp?,
                    messages: MutableList<Any>?,
                    status: ConnectIQ.IQMessageStatus
                ) {
                    if (status == ConnectIQ.IQMessageStatus.SUCCESS) {
                        messages?.forEach { message ->
                            if (message is String) {
                                try {
                                    val jsonMessage = JSONObject(message)
                                    handleMessageFromDevice(jsonMessage)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing message", e)
                                }
                            }
                        }
                    }
                }
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error registering message listener", e)
    }
}
```

### 8. Test the Integration

1. **Install Garmin Connect Mobile** on your Android phone
2. **Pair your Garmin device** with the phone
3. **Install GPS Tracker app** on both Garmin device and phone
4. **Run the Android app** and start tracking
5. **Open Garmin app** and verify data sync

## Message Protocol

The app uses JSON messages for communication:

### Phone → Garmin Messages

#### Speed Update
```json
{
  "type": 1,
  "currentSpeed": 45.5,
  "maxSpeed": 67.2,
  "avgSpeed": 42.1,
  "timestamp": 1234567890
}
```

#### Users Update
```json
{
  "type": 2,
  "users": [
    {
      "userId": "uuid",
      "userName": "John",
      "speed": 45.5,
      "latitude": 40.7128,
      "longitude": -74.0060,
      "bearing": 90.0
    }
  ]
}
```

#### Connection Status
```json
{
  "type": 3,
  "connected": true,
  "gpsActive": true
}
```

#### Tracking State
```json
{
  "type": 4,
  "active": true
}
```

### Garmin → Phone Messages

#### Start Tracking
```json
{
  "type": 101,
  "userId": "uuid",
  "userName": "Garmin User",
  "groupName": "default"
}
```

#### Stop Tracking
```json
{
  "type": 102,
  "userId": "uuid"
}
```

#### Reset Stats
```json
{
  "type": 103,
  "userId": "uuid"
}
```

#### Group Horn
```json
{
  "type": 104,
  "userId": "uuid"
}
```

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│                 │         │                  │         │                 │
│  Garmin Device  │ ◄──────► Garmin Connect   │ ◄──────► │  Android App    │
│  (Connect IQ)   │         │  Mobile App      │         │  (GPS Tracker)  │
│                 │         │                  │         │                 │
└─────────────────┘         └──────────────────┘         └─────────────────┘
        │                                                          │
        │                                                          │
        └─────────────────────────────────────────────────────────┘
                             Bluetooth LE
```

Communication flows:
1. Garmin device connects to Garmin Connect Mobile via Bluetooth
2. Android app discovers Garmin device via ConnectIQ SDK
3. Messages are relayed through Garmin Connect Mobile
4. Both apps can send/receive messages asynchronously

## Best Practices

### Battery Optimization
- Only send updates when data changes significantly
- Batch multiple updates when possible
- Use appropriate message priorities
- Implement message queuing for offline scenarios

### Error Handling
- Always check device connection before sending
- Implement retry logic with exponential backoff
- Handle SDK errors gracefully
- Provide user feedback for connection issues

### Testing
- Test with multiple Garmin device models
- Test connection recovery scenarios
- Test with weak Bluetooth signal
- Test battery impact during extended use
- Test with phone app in background

### User Experience
- Show Garmin connection status in UI
- Provide clear setup instructions
- Handle missing Garmin Connect Mobile app
- Support manual device selection
- Allow disabling Garmin sync

## Permissions

Ensure these permissions are in `AndroidManifest.xml`:

```xml
<!-- Already included, but verify: -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

## Troubleshooting

### SDK Initialization Fails
- Check internet connection (required for first init)
- Verify Garmin Connect Mobile is installed
- Check app ID is correct
- Review Garmin Developer Portal status

### Device Not Found
- Ensure Garmin device is paired with phone
- Check Bluetooth is enabled
- Verify Garmin Connect Mobile is running
- Try manual device refresh

### Messages Not Sending
- Check device connection status
- Verify app is installed on Garmin device
- Check message format is valid JSON
- Review ConnectIQ SDK version compatibility

### Messages Not Receiving
- Verify message listener is registered
- Check app is in foreground on Garmin device
- Review message queue limits
- Check for SDK version mismatches

## Advanced Features

### Multiple Device Support
```kotlin
private val connectedDevices = mutableListOf<IQDevice>()

fun scanForAllDevices() {
    connectedDevices.clear()
    connectedDevices.addAll(connectIQ.connectedDevices)

    connectedDevices.forEach { device ->
        findAppOnDevice(device)
    }
}
```

### Message Queuing
```kotlin
private val messageQueue = ConcurrentLinkedQueue<JSONObject>()

fun queueMessage(message: JSONObject) {
    messageQueue.offer(message)
    processQueue()
}

private fun processQueue() {
    while (!messageQueue.isEmpty() && isDeviceConnected) {
        val message = messageQueue.poll()
        message?.let { sendMessageToDevice(it) }
    }
}
```

### Connection Monitoring
```kotlin
private fun startConnectionMonitoring() {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(object : Runnable {
        override fun run() {
            checkConnection()
            handler.postDelayed(this, 30000) // Check every 30 seconds
        }
    }, 30000)
}

private fun checkConnection() {
    if (currentDevice != null) {
        val status = connectIQ.getDeviceStatus(currentDevice)
        isDeviceConnected = status == IQDevice.IQDeviceStatus.CONNECTED
    }
}
```

## Resources

- [ConnectIQ Mobile SDK Documentation](https://developer.garmin.com/connect-iq/connect-iq-basics/mobile-sdk/)
- [Garmin Developer Forums](https://forums.garmin.com/developer/connect-iq/)
- [Android Sample Apps](https://github.com/garmin/connectiq-companion-app-samples)
- [SDK Reference](https://developer.garmin.com/connect-iq/api-docs/)

## Support

For Garmin-specific issues:
- Contact: [email protected]
- Forums: Garmin Developer Community
- Documentation: developer.garmin.com

For GPS Tracker issues:
- See main project documentation
- Check integration guides
- Review server logs

## Next Steps

After implementing SDK integration:

1. ✅ Test basic connectivity
2. ✅ Verify message sending
3. ✅ Confirm message receiving
4. ✅ Test with multiple devices
5. ✅ Optimize battery usage
6. ✅ Add user-facing controls
7. ✅ Submit for app review
8. ✅ Publish to stores

## License

Same as main GPS Tracker project.
