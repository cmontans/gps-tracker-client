# GPS Speed Tracker Client

A real-time GPS speed tracking application with live telemetry, group sharing, and map visualization. Available as both a web application and a native Android app. Track your speed, view other users in your group, and visualize movement on an interactive map.

## Platform Support

- **Web Application**: Progressive Web App (PWA) for iOS and Android browsers
- **Native Android**: Full-featured Android application (see `android/` directory)

## Features

### Core Functionality
- **Real-time GPS Speed Tracking**: Track your current speed with high accuracy GPS
- **Live Statistics**: View current speed, maximum speed, and average speed
- **User Groups**: Only see and share data with users in your group
- **WebSocket Connection**: Real-time data synchronization with other users
- **Persistent Sessions**: Auto-reconnection with exponential backoff (up to 10 attempts)
- **Keep-Alive System**: Automatic 25-second ping/pong to maintain connection stability
- **Smart Reconnection**: Exponential backoff delays (1s, 2s, 4s, 8s... up to 30s max)

### Map & Visualization
- **Interactive Map**: OpenStreetMap integration via Leaflet
- **Real-time User Positions**: See all group members on the map with directional indicators
- **Directional Arrow Markers**: Rotating arrow icons show direction of travel based on GPS bearing
- **Smart Movement Tracks**: Visual polylines showing user trajectories (only recorded when speed > 3.6 km/h)
- **Track History**: Maintains last 50 position points per user for clean visualization
- **Fullscreen Map Mode**: Dedicated fullscreen view for better visibility
- **Interactive Markers**: Click any user marker to see detailed popup with speed, max speed, and timestamp
- **Compass Directions**: 8-point compass display (N, NE, E, SE, S, SO, O, NO) in user list

### User Experience
- **Dark Mode**: Full dark mode support with automatic persistence (defaults to dark on first use)
- **PWA Support**: Install as a standalone app on iOS and Android
- **Wake Lock**: Keeps screen on during tracking (Android)
- **Group Horn**: Alert all group members with a powerful dual-tone sound notification
- **Mobile Optimized**: Responsive design for all screen sizes with touch-friendly controls
- **GPS Status Indicators**: Visual feedback for connection and GPS status with emoji indicators
- **Precision Speed Display**: All speeds shown with one decimal place (e.g., 45.3 km/h)
- **GPS Accuracy Meter**: Real-time accuracy display in meters
- **Live Coordinates**: Visible GPS coordinates with 6 decimal precision

### KML Network Link (Google Earth Integration)
- **Real-time KML Export**: View all active users in Google Earth, Google Maps, or any KML viewer
- **Auto-refresh NetworkLinks**: User positions automatically update every 5 seconds (configurable)
- **Group Filtering**: View only specific groups or all users worldwide
- **Rich Metadata**: Each marker shows user name, speed, max speed, bearing, and timestamp
- **Status Indicators**: Color-coded freshness indicators (🟢 fresh, 🟡 recent, 🟠 older)
- **Zero Configuration**: Just open the URL in Google Earth - no file downloads needed
- See [KML_NETWORK_LINK.md](KML_NETWORK_LINK.md) for detailed usage instructions

## Technologies Used

- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Styling**: Tailwind CSS (via CDN)
- **Maps**: Leaflet.js with OpenStreetMap tiles
- **Communication**: WebSocket for real-time data
- **APIs**:
  - Geolocation API for GPS tracking
  - Wake Lock API for screen management
  - Web Audio API for horn sounds

## Getting Started

### Prerequisites
- A modern web browser (Chrome, Firefox, Safari, Edge)
- GPS-enabled device (smartphone or tablet recommended)
- Internet connection
- Access to the WebSocket server

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/cmontans/gps-tracker-client.git
   cd gps-tracker-client
   ```

2. **Serve the application**:
   You can use any static file server. Examples:

   ```bash
   # Using Python
   python -m http.server 8000

   # Using Node.js http-server
   npx http-server

   # Using PHP
   php -S localhost:8000
   ```

3. **Access the app**:
   Open your browser and navigate to `http://localhost:8000`

### Installing as PWA

#### iOS (Safari)
1. Open the app in Safari
2. Tap the Share button (square with arrow up)
3. Select "Add to Home Screen"
4. Open the app from your home screen icon

#### Android (Chrome)
1. Open the app in Chrome
2. Tap the menu (three dots)
3. Select "Add to Home Screen" or "Install App"
4. Open the installed app

## Usage

### Basic Setup

1. **Enter Your Name**: Type your name in the "Tu Nombre" field (max 20 characters)
2. **Set Group Name**: Enter a group name (case-sensitive, max 30 characters)
3. **Configure Server** (optional): Click "⚙️ Ajustes" to set a custom WebSocket server URL
4. **Start Tracking**: Press "▶️ Iniciar Seguimiento"
5. **Grant Permissions**: Allow location access when prompted
6. **Go Outside**: For best GPS accuracy, use the app outdoors with clear sky view

### Features Explained

#### Speed Statistics
- **Current Speed**: Real-time speed in km/h with one decimal precision (e.g., 42.7 km/h)
- **Maximum Speed**: Highest speed reached in current session, persists until manual reset
- **Average Speed**: Rolling average of last 20 speed readings for smooth calculation
- **GPS Accuracy**: Live display of GPS accuracy in meters
- **Direction/Bearing**: Automatic calculation and display using compass points (N, NE, E, etc.)
- **Coordinates**: Real-time latitude/longitude with 6 decimal precision

#### Map Features
- **View Map**: Click "🗺️ Ver Mapa" to show the interactive map
- **Fullscreen Mode**: Click "⛶ Pantalla Completa" for a larger map view
- **Clear Tracks**: Remove trajectory lines from the map with "🗑️ Limpiar" button
- **Directional Markers**: Arrow icons that rotate to show direction of travel
- **Smart Tracking**: Trajectory lines only appear when moving (speed > 3.6 km/h)
- **User Info Popups**: Click any marker to see detailed stats (current speed, max speed, timestamp)
- **Color Coding**: Your marker is indigo/blue, other users are green
- **Auto-Center**: Map automatically centers on your position
- **Track Limits**: Each user's track maintains last 50 GPS points for performance

#### Group System
- Users are isolated by group name
- Group names are case-sensitive
- All group members must use the exact same group name
- Perfect for races, group rides, or team tracking

#### Group Horn
- Press "📢 Bocina Grupal" to send an audio alert to all group members
- Must be actively tracking to use
- Plays a distinctive dual-tone horn sound (220Hz + 330Hz sawtooth waves)
- 1.5-second duration with fade-out for realistic car horn effect
- Uses Web Audio API for cross-browser compatibility
- Sender's name displayed in notification on receiving devices

### Settings
Access settings via "⚙️ Ajustes":
- **Server URL**: Configure the WebSocket server endpoint
- **User ID**: View your unique user identifier

### Statistics Reset
Click "🔄 Resetear Estadísticas" to reset max speed and average speed to zero

## Configuration

### Server Connection
Default WebSocket server: `wss://gps-tracker-server-production-5900.up.railway.app`

To use a custom server, click the settings button and enter your WebSocket URL.

### GPS Options
The app uses high accuracy GPS with the following settings:
- `enableHighAccuracy: true` - Maximum GPS precision
- `timeout: 15000` - 15 second timeout
- `maximumAge: 0` - No cached positions

## Platform-Specific Notes

### Android
- Wake Lock automatically keeps the screen on
- Reduce brightness to save battery
- Install as PWA for best performance
- Location permission required

### iOS
- **Important**: Wake Lock not supported - manually set screen timeout
  1. Settings → Display & Brightness → Auto-Lock → "Never"
- Must use Safari for PWA installation
- Grant location permissions:
  1. Settings → Safari → Location → "Allow"
  2. Settings → Privacy → Location Services → Safari → "While Using"

## Troubleshooting

### GPS Not Working
1. Ensure GPS is enabled on your device
2. Grant location permissions when prompted
3. Go outside for better satellite visibility
4. Wait 10-30 seconds for GPS lock

### Connection Issues
1. Verify server URL in settings (⚙️ Ajustes)
2. Check internet connection
3. App auto-reconnects up to 10 times with exponential backoff
4. Keep-alive pings every 25 seconds maintain connection
5. Check browser console for detailed error messages
6. Look for connection status indicator (✅ Conectado / ❌ Desconectado)

### Screen Turns Off (iOS)
Set Auto-Lock to "Never" in Settings → Display & Brightness

## Development

### Project Structure
```
gps-tracker-client/
├── index.html          # Main application file (contains HTML, CSS, JS)
└── README.md          # This file
```

### Key Functions

#### GPS Tracking
- `startTracking()` - Initialize GPS watch and WebSocket connection
- `stopTracking()` - Clean up resources and disconnect
- `calculateBearing()` - Calculate direction between two GPS points using haversine formula
- `requestWakeLock()` - Request screen wake lock (Android)
- `releaseWakeLock()` - Release wake lock on tracking stop

#### Map Management
- `initMap()` - Initialize Leaflet map instance
- `updateMap()` - Refresh user markers and tracks
- `createArrowIcon()` - Generate directional user markers
- `clearMapTracks()` - Remove trajectory polylines

#### WebSocket
- `connectWebSocket()` - Establish server connection with auto-reconnect logic
- `startKeepAlive()` - Initialize 25-second ping interval
- `stopKeepAlive()` - Clear keep-alive interval
- **Message types**:
  - `register` - Initial registration with userId, userName, and groupName
  - `speed` - GPS data update with position, speed, bearing, and timestamp
  - `users` - Server broadcast of all active users in the group
  - `group-horn` - Group notification for horn alerts
  - `ping/pong` - Keep-alive heartbeat messages

#### UI State
- `updateConnectionStatus()` - Update WebSocket status indicator (✅/❌)
- `updateGpsStatus()` - Update GPS status indicator (✅/📍)
- `updateUsersList()` - Refresh user list display with speeds and directions
- `showError()` - Display error messages with 8-second auto-hide
- `showSuccess()` - Display success messages with 3-second auto-hide
- `playHornSound()` - Generate and play horn audio using Web Audio API

### Browser Compatibility
- Chrome/Edge 90+
- Safari 14+
- Firefox 88+
- Mobile browsers with Geolocation API support

## Version History

- **v4.1** - Dark mode with localStorage persistence, fullscreen map viewer, wake lock support, dark mode as default
- **v4.0** - One decimal place precision for all speeds, powerful group horn with dual-tone audio
- **v3.x** - User names displayed in map markers, dark mode set as default theme
- **v3.0** - Direction/bearing calculation, compass point display, arrow rotation on map
- **v2.x** - OpenStreetMap integration, real-time track polylines, smart track recording
- **v1.x** - Initial release with core GPS tracking and WebSocket communication

## License

This project is available for use under standard open source terms. Check with the repository owner for specific licensing details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Native Android App

A fully-featured native Android application is available in the `android/` directory with:

- Native performance and better battery optimization
- Google Maps integration
- Foreground service for reliable tracking
- Material Design UI with dark mode
- Full compatibility with the same WebSocket server

See [android/README.md](android/README.md) for setup instructions.

**Quick Start**:
```bash
cd android
./gradlew build
# Configure Google Maps API key in AndroidManifest.xml
./gradlew installDebug
```

## Server

This client requires a compatible WebSocket server. See the server repository for setup instructions.

Both the web app and Android app use the same WebSocket protocol and can interact with each other in real-time.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

**Note**: This application requires GPS access and an active internet connection to function properly. Accuracy depends on GPS signal quality and device capabilities.
