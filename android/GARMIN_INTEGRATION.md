# Garmin Connect IQ Integration Guide

This document explains how to integrate the GPS Tracker with Garmin smartwatches using Connect IQ.

## Overview

Garmin smartwatches use the **Connect IQ** platform, which is separate from Android. Integration requires:
1. A Connect IQ app running on the Garmin device
2. Communication bridge through the Garmin Connect Mobile app (Android)
3. The GPS Tracker Android app modified to communicate with Garmin Connect Mobile

## Architecture

```
Garmin Watch (Connect IQ App)
    ↓ Bluetooth
Garmin Connect Mobile (Bridge App)
    ↓ IPC/Broadcast
GPS Tracker Android App
    ↓ WebSocket
GPS Tracker Server
```

## Prerequisites

### Development Tools
1. **Connect IQ SDK**: Download from [Garmin Developer Portal](https://developer.garmin.com/connect-iq/sdk/)
2. **Eclipse IDE** or **Visual Studio Code** with Connect IQ extension
3. **Garmin Express**: For device management
4. **Garmin Connect Mobile**: For testing communication

### Garmin Developer Account
- Register at [developer.garmin.com](https://developer.garmin.com)
- Required for app submission and API access

## Connect IQ App Development

### 1. Project Setup

Create a new Connect IQ project with these specifications:

**Project Type**: Widget or Watch Face (for always-on display) or Data Field (during activities)

**Target Devices**:
- Forerunner series (245, 255, 265, 745, 945, 955, 965)
- Fenix series (6, 7, 8)
- Epix series
- Vivoactive series
- Venu series

**API Level**: 3.2.0+ (for modern devices)

### 2. Connect IQ App Structure

```
garmin-gps-tracker/
├── manifest.xml
├── resources/
│   ├── layouts/
│   │   └── main.xml
│   ├── strings/
│   │   └── strings.xml
│   └── drawables/
├── source/
│   ├── GPSTrackerApp.mc
│   ├── GPSTrackerView.mc
│   ├── GPSTrackerDelegate.mc
│   └── CommunicationManager.mc
└── monkey.jungle
```

### 3. Key Features to Implement

#### A. Location Tracking
```monkey-c
using Toybox.Position;
using Toybox.System;

class GPSTrackerApp extends Application.AppBase {
    var locationListener;

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state) {
        Position.enableLocationEvents(
            Position.LOCATION_CONTINUOUS,
            method(:onPosition)
        );
    }

    function onPosition(info) {
        var speed = info.speed * 3.6; // m/s to km/h
        var lat = info.position.toDegrees()[0];
        var lon = info.position.toDegrees()[1];

        // Send to phone via Communication API
        sendLocationUpdate(speed, lat, lon);
    }
}
```

#### B. Communication with Android App
```monkey-c
using Toybox.Communications;

function sendLocationUpdate(speed, lat, lon) {
    var params = {
        "type" => "location",
        "speed" => speed,
        "latitude" => lat,
        "longitude" => lon,
        "timestamp" => System.getTimer()
    };

    Communications.transmit(
        params,
        null,
        new CommunicationListener()
    );
}

class CommunicationListener extends Communications.ConnectionListener {
    function onComplete() {
        System.println("Data sent successfully");
    }

    function onError() {
        System.println("Communication error");
    }
}
```

#### C. Display View
```monkey-c
using Toybox.WatchUi;
using Toybox.Graphics;

class GPSTrackerView extends WatchUi.View {
    var speed = 0.0;
    var maxSpeed = 0.0;
    var avgSpeed = 0.0;

    function initialize() {
        View.initialize();
    }

    function onUpdate(dc) {
        dc.clear();

        // Draw speed display
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.drawText(
            dc.getWidth() / 2,
            dc.getHeight() / 3,
            Graphics.FONT_NUMBER_THAI_HOT,
            speed.format("%.1f"),
            Graphics.TEXT_JUSTIFY_CENTER
        );

        dc.drawText(
            dc.getWidth() / 2,
            dc.getHeight() / 2,
            Graphics.FONT_SMALL,
            "km/h",
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }
}
```

### 4. Manifest Configuration

**manifest.xml**:
```xml
<iq:manifest xmlns:iq="http://www.garmin.com/xml/connectiq" version="3">
    <iq:application entry="GPSTrackerApp" id="gps-tracker-app"
                    launcherIcon="@Drawables.LauncherIcon"
                    minApiLevel="3.2.0"
                    name="@Strings.AppName"
                    type="widget">

        <iq:products>
            <iq:product id="fenix7"/>
            <iq:product id="forerunner255"/>
            <iq:product id="vivoactive4"/>
            <!-- Add more devices as needed -->
        </iq:products>

        <iq:permissions>
            <iq:uses-permission id="Position"/>
            <iq:uses-permission id="Communications"/>
            <iq:uses-permission id="SensorHistory"/>
        </iq:permissions>

        <iq:languages>
            <iq:language>eng</iq:language>
        </iq:languages>
    </iq:application>
</iq:manifest>
```

## Android App Integration

### 1. Add Garmin SDK Support

Update `app/build.gradle`:
```gradle
dependencies {
    // Garmin Connect IQ Communication
    implementation 'com.garmin.connectiq:ciq-companion-app-sdk:2.0.3'
}
```

### 2. Create Garmin Communication Service

**GarminCommunicationService.kt**:
```kotlin
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQApp

class GarminCommunicationService(private val context: Context) {
    private var connectIQ: ConnectIQ? = null
    private var connectedDevices = mutableListOf<IQDevice>()

    fun initialize() {
        connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS)

        connectIQ?.initialize(context, true, object : ConnectIQ.ConnectIQListener {
            override fun onSdkReady() {
                loadConnectedDevices()
            }

            override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                Log.e(TAG, "Garmin SDK init error: $status")
            }

            override fun onSdkShutDown() {
                // Handle shutdown
            }
        })
    }

    private fun loadConnectedDevices() {
        connectIQ?.connectedDevices?.let { devices ->
            connectedDevices.clear()
            connectedDevices.addAll(devices)
        }
    }

    fun sendLocationUpdate(speed: Double, lat: Double, lon: Double) {
        val data = mapOf(
            "type" to "location",
            "speed" to speed,
            "latitude" to lat,
            "longitude" to lon
        )

        connectedDevices.forEach { device ->
            sendMessageToDevice(device, data)
        }
    }

    private fun sendMessageToDevice(device: IQDevice, data: Map<String, Any>) {
        // Implementation depends on your Connect IQ app ID
    }
}
```

### 3. Register Receiver in AndroidManifest.xml

```xml
<receiver android:name=".receiver.GarminDataReceiver" android:exported="true">
    <intent-filter>
        <action android:name="com.garmin.android.apps.connectmobile.CONNECTIQ_MESSAGE_RECEIVED"/>
    </intent-filter>
</receiver>
```

## Communication Protocol

### From Watch to Phone

**Location Update**:
```json
{
    "type": "location",
    "speed": 45.5,
    "latitude": 37.7749,
    "longitude": -122.4194,
    "timestamp": 123456789
}
```

**Control Commands**:
```json
{
    "type": "control",
    "command": "start|stop|horn|reset"
}
```

### From Phone to Watch

**Group Members**:
```json
{
    "type": "users",
    "users": [
        {"name": "User1", "speed": 30.5},
        {"name": "User2", "speed": 42.0}
    ]
}
```

**Stats Update**:
```json
{
    "type": "stats",
    "maxSpeed": 55.3,
    "avgSpeed": 38.2
}
```

## Building and Testing

### 1. Build Connect IQ App

```bash
# Using Connect IQ CLI
monkeyc -o gps-tracker.prg \
        -f monkey.jungle \
        -y developer_key \
        -d fenix7

# Or use Eclipse/VSCode build commands
```

### 2. Install on Device

**Simulator**:
```bash
connectiq
# Load .prg file in simulator
```

**Physical Device**:
1. Connect device via USB
2. Use Garmin Express or Eclipse to sideload
3. For production, submit to Connect IQ Store

### 3. Test Communication

1. Install Garmin Connect Mobile on Android phone
2. Pair Garmin watch with phone
3. Run GPS Tracker Android app
4. Start tracking on watch
5. Verify data appears in Android app

## Limitations

### Technical Constraints
- **Battery**: Continuous GPS tracking drains battery quickly
- **Communication**: Relies on Bluetooth connection to phone
- **Data Rate**: Limited to periodic updates (not real-time)
- **API Access**: Some Garmin features require approval
- **Storage**: Limited memory on watch devices

### Device Support
- Not all Garmin devices support Connect IQ
- Different devices have different API capabilities
- Some older devices lack continuous GPS mode

## Alternative Approach: ANT+ Protocol

For more direct integration without Garmin Connect Mobile:

### Pros
- Direct communication between devices
- Lower latency
- No dependency on Garmin app

### Cons
- Requires ANT+ SDK license
- More complex implementation
- Limited to ANT+ capable Android devices
- Requires custom hardware protocol

## Publishing to Connect IQ Store

1. **Prepare Assets**:
   - App icon (various sizes)
   - Screenshots from different devices
   - App description and changelog
   - Privacy policy

2. **App Validation**:
   - Must pass Connect IQ app validator
   - No crashes or memory leaks
   - Follows UI guidelines

3. **Submission**:
   - Upload via [Garmin Developer Portal](https://apps.garmin.com/developer)
   - Review process takes 1-2 weeks
   - Updates require re-submission

## Resources

- [Connect IQ Developer Guide](https://developer.garmin.com/connect-iq/connect-iq-basics/)
- [API Documentation](https://developer.garmin.com/connect-iq/api-docs/)
- [Connect IQ SDK Download](https://developer.garmin.com/connect-iq/sdk/)
- [Sample Apps](https://github.com/garmin/connectiq-samples)
- [Developer Forum](https://forums.garmin.com/developer/)

## Next Steps

1. Set up Connect IQ development environment
2. Create basic Connect IQ app with GPS tracking
3. Implement communication with Android app
4. Test on simulator and physical device
5. Optimize battery usage
6. Submit to Connect IQ Store (optional)

## Notes

- Garmin integration is more complex than Wear OS due to separate platform
- Consider starting with Wear OS support first
- Garmin Connect IQ apps require separate maintenance and updates
- Battery life is a major consideration for continuous GPS tracking
- Some features may require Garmin developer approval
