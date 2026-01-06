# GPS Speed Tracker - Native Android App

A native Android application for real-time GPS speed tracking with live telemetry, group sharing, and map visualization. This app is compatible with the same WebSocket server as the web client.

## Features

### Core Functionality
- **Real-time GPS Speed Tracking**: High accuracy GPS tracking with speed in km/h
- **Live Statistics**: Current speed, maximum speed, and rolling average speed
- **User Groups**: Share data only with users in your group
- **WebSocket Connection**: Real-time data synchronization with other users
- **Persistent Sessions**: Auto-reconnection with exponential backoff (up to 10 attempts)

### Map & Visualization
- **Google Maps Integration**: Interactive map with real-time user positions
- **User Markers**: Display user names, speeds, and directions on the map
- **Movement Tracks**: Visual polylines showing user trajectories
- **Clear Tracks**: Remove trajectory lines from the map
- **Auto-centering**: Camera follows your position

### User Experience
- **Dark Mode**: Full dark mode support (default enabled)
- **Foreground Service**: Continuous tracking with notification
- **Wake Lock**: Keeps screen on during tracking
- **Group Horn**: Alert all group members with a notification
- **Multi-language Support**: English and Spanish
- **Material Design**: Modern UI with Material Components

## Technologies Used

- **Language**: Kotlin
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: Service-based architecture with foreground service
- **Maps**: Google Maps Android API
- **Location**: Google Play Services Location API
- **Communication**: Java-WebSocket library
- **JSON**: Gson for serialization/deserialization
- **UI**: Material Components, ViewBinding

## Setup Instructions

### Prerequisites
- Android Studio (Latest version recommended)
- Android SDK with API 24+
- Google Maps API Key
- Device or emulator with Google Play Services

### Installation

1. **Clone the repository**:
   ```bash
   cd gps-tracker-client/android
   ```

2. **Get Google Maps API Key**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one
   - Enable "Maps SDK for Android"
   - Create credentials (API Key)
   - Restrict the key to Android apps (recommended)

3. **Configure API Key**:
   Edit `app/src/main/AndroidManifest.xml` and replace:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
   ```
   with your actual API key.

4. **Build the project**:
   ```bash
   ./gradlew build
   ```

5. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

   Or use Android Studio:
   - Open the `android` directory as a project
   - Wait for Gradle sync
   - Click Run (Shift+F10)

## Usage

### First Time Setup

1. **Launch the app**
2. **Enter your name** (max 20 characters)
3. **Enter group name** (case-sensitive, max 30 characters)
4. **Tap "Start Tracking"**
5. **Grant permissions**:
   - Location (required)
   - Notifications (Android 13+, required for foreground service)

### Tracking Features

#### Speed Statistics
- **Current Speed**: Real-time speed with 0.1 km/h precision
- **Maximum Speed**: Highest speed in current session
- **Average Speed**: Rolling average of last 20 readings

#### Map Features
- **View Map**: Toggle map visibility
- **User Markers**: Shows all group members with their speed and direction
- **Clear Tracks**: Remove all trajectory polylines
- **Auto-follow**: Map centers on your current position

#### Group Features
- **Group Isolation**: Only see users in the same group (case-sensitive)
- **Group Horn**: Send notification alert to all group members
- **Users List**: View all active users with their current speed

### Settings
- **Server URL**: Configure WebSocket server endpoint (default: wss://fatal-robinette-cmontans-2b748130.koyeb.app)
- **User ID**: View your unique identifier (auto-generated)

### Controls
- **Reset Statistics**: Clear max and average speed
- **Stop Tracking**: End session and disconnect

## Permissions

The app requires the following permissions:

- `ACCESS_FINE_LOCATION`: For high-accuracy GPS tracking
- `ACCESS_COARSE_LOCATION`: Backup location permission
- `INTERNET`: WebSocket communication
- `ACCESS_NETWORK_STATE`: Check connection status
- `WAKE_LOCK`: Keep screen on during tracking
- `FOREGROUND_SERVICE`: Run tracking in background
- `FOREGROUND_SERVICE_LOCATION`: Specify foreground service type
- `POST_NOTIFICATIONS`: Show tracking notification (Android 13+)

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/tracker/gps/
│   │   │   ├── MainActivity.kt              # Main activity with UI
│   │   │   ├── SettingsActivity.kt          # Settings screen
│   │   │   ├── UsersAdapter.kt              # RecyclerView adapter
│   │   │   ├── model/
│   │   │   │   ├── UserData.kt              # User data model
│   │   │   │   └── WebSocketMessage.kt      # WebSocket messages
│   │   │   ├── service/
│   │   │   │   └── LocationTrackingService.kt # Foreground service
│   │   │   └── websocket/
│   │   │       └── WebSocketClient.kt       # WebSocket client
│   │   ├── res/
│   │   │   ├── layout/                      # XML layouts
│   │   │   ├── values/                      # English strings, colors, themes
│   │   │   ├── values-es/                   # Spanish strings
│   │   │   ├── values-night/                # Dark theme
│   │   │   └── xml/                         # Network config
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

## Key Features Implementation

### GPS Tracking
- High accuracy mode with 1-second update interval
- Speed calculated from GPS velocity (m/s → km/h)
- Bearing/direction tracking for map markers

### WebSocket Protocol
The app uses the same protocol as the web client:

**Register**:
```json
{
  "type": "register",
  "userId": "uuid",
  "userName": "User Name",
  "groupName": "Group Name"
}
```

**Speed Update**:
```json
{
  "type": "speed",
  "userId": "uuid",
  "speed": 45.5,
  "latitude": 40.7128,
  "longitude": -74.0060,
  "bearing": 180.0
}
```

**Users Update** (received):
```json
{
  "type": "users",
  "users": [...]
}
```

**Group Horn**:
```json
{
  "type": "group-horn",
  "userId": "uuid"
}
```

### Foreground Service
- Runs continuously while tracking
- Shows persistent notification with current speed
- Handles WebSocket connection and GPS updates
- Survives app backgrounding

### Wake Lock
- Keeps screen on during tracking
- Uses SCREEN_BRIGHT_WAKE_LOCK
- Automatically released when tracking stops
- Battery-intensive - recommend reducing brightness

## Troubleshooting

### GPS Not Working
1. Enable Location Services on device
2. Grant Location permissions
3. Go outdoors for better satellite visibility
4. Wait 10-30 seconds for GPS lock
5. Ensure "High Accuracy" mode in device settings

### Map Not Showing
1. Verify Google Maps API key is configured
2. Check API key restrictions match package name
3. Enable "Maps SDK for Android" in Google Cloud Console
4. Ensure device has Google Play Services

### Connection Issues
1. Check internet connection
2. Verify server URL in settings
3. Check app logs for WebSocket errors
4. App auto-reconnects up to 10 times with backoff

### Permissions Denied
1. Go to Settings → Apps → GPS Speed Tracker → Permissions
2. Enable Location (set to "Allow all the time" for best experience)
3. Enable Notifications (Android 13+)

## Building Release APK

1. **Generate signing key**:
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure signing** in `app/build.gradle`

3. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

4. **APK location**: `app/build/outputs/apk/release/app-release.apk`

## Performance Considerations

- **Battery Usage**: GPS and wake lock are battery-intensive
  - Reduce screen brightness
  - Close when not needed
- **Data Usage**: WebSocket sends updates every 1-2 seconds
  - Minimal data usage (~1-2 MB/hour)
- **Background Restrictions**: Some devices aggressively kill background services
  - Add app to battery optimization exceptions
  - Disable "Adaptive Battery" for this app

## Compatibility

- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 14 (API 34)
- **Tested**: Android 8.0, 10, 11, 12, 13, 14
- **Google Play Services**: Required for Maps and Location

## Known Limitations

1. **Google Maps API Key Required**: Need to set up your own API key
2. **Horn Sound**: Currently shows toast notification instead of playing sound
3. **Fullscreen Map**: Not yet implemented
4. **Battery**: Significant battery usage due to continuous GPS and wake lock

## Future Enhancements

- [ ] Implement actual horn sound playback
- [ ] Add fullscreen map activity
- [ ] Export session data (GPX, CSV)
- [ ] Speed alerts/warnings
- [ ] Configurable GPS update intervals
- [ ] Battery optimization options
- [ ] Offline map caching
- [ ] Session history
- [ ] Speed unit preferences (km/h, mph, knots)

## Server Compatibility

This app is fully compatible with the GPS Tracker Server:
- Default server: `wss://fatal-robinette-cmontans-2b748130.koyeb.app`
- Custom servers supported via Settings
- Same protocol as web client
- Group isolation works across all clients

## License

This project is available for use under standard open source terms. Check with the repository owner for specific licensing details.

## Contributing

Contributions are welcome! Areas for contribution:
- Improve battery efficiency
- Add sound effects for horn
- Implement fullscreen map
- UI/UX improvements
- Bug fixes

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

**Note**: This app requires continuous GPS and network access. Use responsibly and be aware of battery consumption. Accuracy depends on GPS signal quality and device capabilities.
