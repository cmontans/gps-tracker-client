## Smartwatch Integration

The GPS Tracker app now supports both **Android Wear OS** and **Garmin** smartwatches, allowing you to track your location and view group members directly from your wrist.

## Features

### Core Functionality
- ✅ Real-time GPS speed tracking on watch
- ✅ Display current, max, and average speed
- ✅ View group members and their speeds
- ✅ Start/Stop tracking from watch
- ✅ Trigger group horn
- ✅ Reset statistics
- ✅ Connection status indicators (GPS, Server, Phone)

### Operating Modes

#### 1. **Companion Mode** (Watch + Phone)
- Watch communicates with phone via Bluetooth
- Phone handles WebSocket connection to server
- Lower battery usage on watch
- Extended range through phone's connection

#### 2. **Standalone Mode** (Watch Only)
- Watch connects directly to server via WiFi/LTE
- No phone required
- Works independently if watch has connectivity
- Ideal for LTE-enabled smartwatches

## Wear OS Integration

### Requirements
- Android phone with Wear OS app
- Wear OS 2.0+ smartwatch (API 26+)
- Bluetooth connection between phone and watch
- (Optional) LTE-enabled watch for standalone mode

### Supported Devices
- Samsung Galaxy Watch 4/5/6
- Google Pixel Watch 1/2
- TicWatch Pro 3/5
- Fossil Gen 6
- Mobvoi TicWatch E3
- Other Wear OS 2.0+ devices

### Installation

#### Automatic Installation (Recommended)
1. Install GPS Tracker on your Android phone
2. The Wear OS app will automatically sync to your paired watch
3. Open "GPS Tracker" on your watch
4. Grant location permissions when prompted

#### Manual Installation (Development)
1. Enable Developer Options on watch
2. Connect watch to computer via ADB
3. Install APK:
   ```bash
   adb -s <watch-device-id> install wear-release.apk
   ```

### Setup
1. **First Launch**:
   - Open app on watch
   - Grant location permissions
   - Grant notification permissions (Android 13+)

2. **Pair with Phone** (for Companion Mode):
   - Ensure phone app is installed
   - Check Bluetooth connection in Wear OS app
   - Watch will automatically detect phone

3. **Start Tracking**:
   - Press "Start" button on watch
   - Enter name and group (default: "Watch User", "Default Group")
   - GPS indicator will turn green when location is acquired
   - Speed will update in real-time

### UI Overview

#### Main Screen
```
┌────────────────────┐
│  🟢Connected  🟢GPS │
├────────────────────┤
│                    │
│    Speed: 45.5     │
│       km/h         │
│                    │
├─────────┬──────────┤
│ Max: 55 │ Avg: 38  │
└─────────┴──────────┘
│  [Start/Stop]      │
│  [Horn]            │
│  [Group]           │
│  [Reset]           │
└────────────────────┘
```

#### Group Members Screen
```
┌────────────────────┐
│  Group Members     │
├────────────────────┤
│ User 1             │
│ 42.5 km/h          │
├────────────────────┤
│ User 2             │
│ 38.0 km/h          │
├────────────────────┤
│ User 3             │
│ 50.2 km/h          │
└────────────────────┘
```

### Navigation
- **Swipe Right**: Go back
- **Swipe Up/Down**: Scroll
- **Tap**: Select button
- **Long Press**: (reserved for future features)

### Battery Optimization

#### Companion Mode (Recommended)
- **Battery Life**: 8-12 hours continuous tracking
- Watch GPS only, server connection via phone
- Recommended for day trips

#### Standalone Mode
- **Battery Life**: 4-6 hours continuous tracking
- Watch GPS + WiFi/LTE
- Recommended for short activities

#### Tips to Extend Battery Life
1. Use Companion Mode when possible
2. Enable battery saver mode when not tracking
3. Reduce screen brightness
4. Use ambient mode during tracking
5. Close other apps running in background

### Troubleshooting

#### Watch Not Connecting to Phone
1. Check Bluetooth is enabled on both devices
2. Ensure Wear OS app shows "Connected" status
3. Restart Wear OS app on phone
4. Restart both phone and watch
5. Re-pair devices in Wear OS app

#### GPS Not Working
1. Ensure location permission is granted
2. Go outdoors for better satellite reception
3. Wait 30-60 seconds for initial GPS lock
4. Check if watch supports GPS (some budget models don't)
5. Restart watch

#### Speed Not Updating
1. Check GPS indicator is green
2. Verify you're moving (GPS requires motion for speed)
3. Check phone app is running (in Companion Mode)
4. Verify WebSocket connection is active

#### App Not Syncing to Watch
1. Ensure phone and watch are paired
2. Check Wear OS app on phone is updated
3. Manually sync apps in Wear OS settings
4. Reinstall phone app
5. Check watch storage isn't full

## Garmin Integration

Garmin watches use the **Connect IQ** platform, which is separate from Android Wear OS.

### Requirements
- Android phone with GPS Tracker app
- Garmin Connect Mobile app
- Compatible Garmin watch (see list below)
- Bluetooth connection between phone and watch

### Supported Devices
- Forerunner: 245, 255, 265, 745, 945, 955, 965
- Fenix: 6, 7, 8 series
- Epix: 2 series
- Vivoactive: 4, 5
- Venu: 2, 3
- Other Connect IQ 3.2.0+ devices

### Installation

**Note**: Garmin integration requires a separate Connect IQ app to be developed and published to the Garmin Connect IQ Store.

#### Current Status
🚧 **In Development** - Garmin Connect IQ app is not yet available. Please refer to [GARMIN_INTEGRATION.md](./GARMIN_INTEGRATION.md) for detailed development guide.

#### Planned Features
1. GPS speed tracking on Garmin device
2. Display group members
3. Communication with Android app via Garmin Connect Mobile
4. Start/Stop tracking from watch
5. Group horn functionality

See [GARMIN_INTEGRATION.md](./GARMIN_INTEGRATION.md) for complete Garmin development guide.

## Development

### Project Structure
```
android/
├── app/                      # Phone app
│   ├── src/main/java/com/tracker/gps/
│   └── service/
│       ├── LocationTrackingService.kt
│       ├── WearableDataService.kt
│       └── PhoneDataLayerListenerService.kt
├── wear/                     # Wear OS app
│   ├── src/main/java/com/tracker/gps/wear/
│   │   ├── MainActivity.kt
│   │   └── service/
│   │       ├── WearLocationService.kt
│   │       └── DataLayerListenerService.kt
└── shared/                   # Shared code/models
    └── src/main/java/com/tracker/gps/shared/
        ├── model/
        │   ├── UserData.kt
        │   ├── TrackPoint.kt
        │   ├── TrackSession.kt
        │   ├── WebSocketMessages.kt
        │   └── WearMessages.kt
        ├── db/
        │   ├── TrackerDatabase.kt
        │   └── TrackDao.kt
        └── util/
            ├── Constants.kt
            └── DataSerializer.kt
```

### Building from Source

#### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34
- Wear OS emulator or physical device

#### Build Steps

1. **Clone Repository**:
   ```bash
   git clone https://github.com/your-repo/gps-tracker-client.git
   cd gps-tracker-client/android
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - File → Open → Select `android` folder

3. **Sync Gradle**:
   - Wait for Gradle sync to complete
   - If errors occur, File → Invalidate Caches → Restart

4. **Build Phone App**:
   ```bash
   ./gradlew :app:assembleDebug
   # Output: app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Build Wear OS App**:
   ```bash
   ./gradlew :wear:assembleDebug
   # Output: wear/build/outputs/apk/debug/wear-debug.apk
   ```

6. **Install**:
   ```bash
   # Phone
   adb install app/build/outputs/apk/debug/app-debug.apk

   # Watch (find device ID with 'adb devices')
   adb -s <watch-device-id> install wear/build/outputs/apk/debug/wear-debug.apk
   ```

### Development with Emulator

#### Phone Emulator
- Standard Android emulator
- Any API level 24+ device

#### Wear OS Emulator
1. Open AVD Manager
2. Create Device → Wear OS
3. Choose: Wear OS Large Round (API 30+)
4. Start emulator

#### Testing Communication
1. Start both emulators
2. Pair emulators:
   ```bash
   adb -d forward tcp:5601 tcp:5601
   ```
3. Open Wear OS app on phone emulator
4. Follow pairing instructions

### API Documentation

#### Wearable Data Layer Paths
```kotlin
// From WearMessages.kt
WearPaths.TRACKING_STATE      = "/tracking/state"
WearPaths.LOCATION_UPDATE     = "/tracking/location"
WearPaths.USERS_UPDATE        = "/tracking/users"
WearPaths.CONTROL_COMMAND     = "/tracking/control"
WearPaths.CONNECTION_STATUS   = "/tracking/connection"
WearPaths.STATS_UPDATE        = "/tracking/stats"
WearPaths.GROUP_HORN          = "/tracking/horn"
```

#### Message Format
```kotlin
// Tracking State
TrackingState(
    isTracking: Boolean,
    userName: String,
    groupName: String,
    currentSpeed: Double,
    maxSpeed: Double,
    avgSpeed: Double,
    timestamp: Long
)

// User Data
UserData(
    userId: String,
    userName: String,
    speed: Double,
    latitude: Double,
    longitude: Double,
    bearing: Float,
    timestamp: Long,
    groupName: String
)

// Control Commands
sealed class WearControlCommand {
    data class StartTracking(userName: String, groupName: String)
    object StopTracking
    object TriggerGroupHorn
    object ResetStats
}
```

### Contributing

Contributions are welcome! Areas for improvement:

1. **Wear OS**:
   - Add map view with Wear OS Maps API
   - Implement complications for watch faces
   - Add tile support for quick access
   - Heart rate integration
   - Voice commands support

2. **Garmin**:
   - Complete Connect IQ app development
   - Implement full communication protocol
   - Add data field support for activities
   - Battery optimization

3. **General**:
   - Offline track storage and sync
   - Historical track viewing
   - Export tracks to GPX format
   - Multi-watch support (paired with single phone)

### Testing

#### Unit Tests
```bash
./gradlew test
```

#### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

#### Manual Testing Checklist
- [ ] Install phone app
- [ ] Install wear app
- [ ] Pair devices
- [ ] Grant permissions
- [ ] Start tracking on watch
- [ ] Verify GPS lock
- [ ] Check speed updates
- [ ] Test group members list
- [ ] Test group horn
- [ ] Test reset stats
- [ ] Test stop tracking
- [ ] Verify data syncs to phone
- [ ] Test standalone mode (if LTE watch)
- [ ] Test battery usage
- [ ] Test reconnection after disconnect

## Privacy & Permissions

### Permissions Used

#### Phone App
- `ACCESS_FINE_LOCATION`: GPS tracking
- `ACCESS_COARSE_LOCATION`: Network location
- `INTERNET`: WebSocket connection
- `ACCESS_NETWORK_STATE`: Connection checking
- `WAKE_LOCK`: Keep app running
- `FOREGROUND_SERVICE`: Background tracking
- `POST_NOTIFICATIONS`: Tracking notifications

#### Wear OS App
Same permissions as phone, plus:
- `BODY_SENSORS`: Future heart rate integration
- `ACTIVITY_RECOGNITION`: Activity type detection

### Data Privacy
- Location data is only shared within your group
- No data is stored on servers permanently
- All communication is encrypted (WSS)
- You control when tracking starts/stops
- No third-party analytics or tracking

## FAQ

**Q: Does the watch need to be connected to phone at all times?**
A: In Companion Mode, yes. In Standalone Mode (LTE watches), no.

**Q: Can I use multiple watches with one phone?**
A: Currently only one watch is actively supported, but multiple watches can receive updates.

**Q: Does this drain my watch battery quickly?**
A: Continuous GPS tracking uses significant battery. Expect 4-12 hours depending on mode and device.

**Q: Can I track without my phone?**
A: Yes, if your watch has WiFi/LTE and supports standalone mode.

**Q: Will this work with my Garmin watch?**
A: Garmin support is in development. See [GARMIN_INTEGRATION.md](./GARMIN_INTEGRATION.md) for status.

**Q: Can I see the map on my watch?**
A: Not yet. Map view is planned for future updates.

**Q: Does it work offline?**
A: GPS works offline, but you need internet to sync with group members.

**Q: Can I export my tracks?**
A: Track export feature is planned for future updates.

## Support

For issues or questions:
- **Issues**: [GitHub Issues](https://github.com/your-repo/gps-tracker-client/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/gps-tracker-client/discussions)
- **Email**: support@your-domain.com

## License

Same as main project license.

## Acknowledgments

- Google Wear OS team for Wearable Data Layer API
- Garmin for Connect IQ platform
- Android Jetpack Compose team
- All contributors and testers
