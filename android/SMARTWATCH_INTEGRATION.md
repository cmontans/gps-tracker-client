# Smartwatch Integration for GPS Tracker

This document describes the smartwatch (Wear OS) integration added to the GPS Tracker Android app.

## Overview

The GPS Tracker app now supports Android smartwatches running Wear OS. Users can:
- View GPS tracking data on their smartwatch
- Control tracking from the watch (start/stop, reset stats, send group horn)
- Use the watch as a standalone tracker or in companion mode with the phone app
- Add GPS speed complications to their watch face

## Architecture

The project has been reorganized into three modules:

### 1. **Shared Module** (`/shared`)
Contains code and data models shared between the phone and watch apps:
- `UserData` - GPS tracking data model
- `WebSocketMessage` - WebSocket protocol messages
- `Constants` - Shared constants for data layer paths, keys, and settings
- `SpeedFormatter` - Utility for formatting speed values

### 2. **App Module** (`/app`)
The existing phone app, enhanced with:
- `PhoneDataLayerService` - Sends tracking data to the watch
- `PhoneMessageListenerService` - Receives control messages from the watch
- Updated to use shared data models

### 3. **Wear Module** (`/wear`)
The new smartwatch app with:
- `MainActivity` - Watch UI for viewing speed and controlling tracking
- `WearLocationTrackingService` - Location tracking service for standalone mode
- `WearDataLayerService` - Sends control messages to the phone
- `WearDataListenerService` - Receives tracking data from the phone
- `SpeedComplicationService` - Watch face complication for quick speed access

## Features

### Watch UI
- **Speed Display**: Large, easy-to-read current speed
- **Statistics**: Max and average speed
- **Status Indicators**: GPS and connection status
- **Control Buttons**:
  - Start/Stop tracking
  - Reset statistics
  - Send group horn

### Watch Complications
Users can add GPS speed to their watch face in three formats:
- **Short Text**: Shows current speed (e.g., "45.0")
- **Long Text**: Shows "Speed: 45.0 km/h"
- **Ranged Value**: Shows speed as a gauge from 0 to max speed

### Data Synchronization

The phone and watch communicate via the Wearable Data Layer API:

**Phone → Watch:**
- Speed updates (current, max, average)
- User list updates
- Connection status
- GPS status
- Tracking state

**Watch → Phone:**
- Start tracking command
- Stop tracking command
- Reset statistics command
- Group horn command

## Usage Modes

### 1. Companion Mode (Recommended)
- Phone app handles GPS tracking and server communication
- Watch displays data and provides controls
- More battery efficient
- Requires phone nearby with Bluetooth connection

### 2. Standalone Mode
- Watch performs its own GPS tracking
- Useful if phone is not available
- Uses more battery on the watch
- Limited to local tracking data

## Technical Details

### Dependencies Added
- `com.google.android.gms:play-services-wearable:18.1.0` - Data Layer API
- `androidx.wear:wear:1.3.0` - Wear OS components
- `androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1` - Complications
- `androidx.health:health-services-client:1.0.0-rc02` - Health Services (for future enhancements)
- `androidx.localbroadcastmanager:localbroadcastmanager:1.1.0` - Inter-component communication

### Permissions
The watch app requires:
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - For GPS tracking
- `INTERNET` / `ACCESS_NETWORK_STATE` - For server communication
- `FOREGROUND_SERVICE` - For background tracking
- `POST_NOTIFICATIONS` - For Android 13+
- `BODY_SENSORS` - For potential future health integration

### Data Layer Paths
Defined in `shared/Constants.kt`:
- `/tracking_state` - Tracking active/inactive
- `/speed_update` - Speed data from phone to watch
- `/users_update` - User list updates
- `/start_tracking` - Start command from watch to phone
- `/stop_tracking` - Stop command from watch to phone
- `/reset_stats` - Reset statistics command
- `/group_horn` - Group horn command
- `/connection_status` - WebSocket connection status

## Building

To build all modules:
```bash
./gradlew assembleDebug
```

To build only the wear module:
```bash
./gradlew :wear:assembleDebug
```

To build only the phone app:
```bash
./gradlew :app:assembleDebug
```

## Installation

### On Emulator/Physical Devices:

1. **Install on phone:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Install on watch:**
   ```bash
   adb -e install wear/build/outputs/apk/debug/wear-debug.apk
   ```

### Pairing Phone and Watch

1. Enable Bluetooth on both devices
2. On the phone, install "Wear OS" app from Play Store
3. Follow pairing instructions
4. Both GPS Tracker apps will automatically sync when running

## Future Enhancements

Possible improvements:
- Ambient mode support for always-on display
- Tile support for quick actions
- Health Services integration for more accurate tracking
- Offline map caching on watch
- Voice commands for hands-free control
- Vibration alerts for speed milestones
- Battery optimization options

## Troubleshooting

### Watch not receiving data:
1. Ensure Bluetooth is enabled and devices are paired
2. Check that both apps are running
3. Verify in phone app settings that data sync is enabled
4. Restart both apps

### GPS not working on watch:
1. Ensure location permission is granted
2. Move to an area with clear sky view
3. Try toggling GPS off and on
4. Check if phone GPS is working (companion mode)

### Complications not updating:
1. Long-press watch face to enter edit mode
2. Remove and re-add the complication
3. Ensure GPS Tracker app is running
4. Check watch battery saver mode settings

## License

Same as the main GPS Tracker project.
