# GPS Speed Tracker Client

A real-time GPS speed tracking web application with live telemetry, group sharing, and map visualization. Track your speed, view other users in your group, and visualize movement on an interactive map.

## Features

### Core Functionality
- **Real-time GPS Speed Tracking**: Track your current speed with high accuracy GPS
- **Live Statistics**: View current speed, maximum speed, and average speed
- **User Groups**: Only see and share data with users in your group
- **WebSocket Connection**: Real-time data synchronization with other users
- **Persistent Sessions**: Auto-reconnection with exponential backoff

### Map & Visualization
- **Interactive Map**: OpenStreetMap integration via Leaflet
- **Real-time User Positions**: See all group members on the map with directional indicators
- **Movement Tracks**: Visual polylines showing user trajectories
- **Fullscreen Map Mode**: Dedicated fullscreen view for better visibility
- **User Markers**: Display user names, speeds, and directions on the map

### User Experience
- **Dark Mode**: Full dark mode support with automatic persistence
- **PWA Support**: Install as a standalone app on iOS and Android
- **Wake Lock**: Keeps screen on during tracking (Android)
- **Group Horn**: Alert all group members with a sound notification
- **Mobile Optimized**: Responsive design for all screen sizes
- **GPS Status Indicators**: Visual feedback for connection and GPS status

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
- **Current Speed**: Real-time speed in km/h with one decimal precision
- **Maximum Speed**: Highest speed reached in current session
- **Average Speed**: Rolling average of last 20 speed readings

#### Map Features
- **View Map**: Click "🗺️ Ver Mapa" to show the interactive map
- **Fullscreen Mode**: Click "⛶ Pantalla Completa" for a larger map view
- **Clear Tracks**: Remove trajectory lines from the map
- **User Markers**: Show position, speed, direction, and name for each user

#### Group System
- Users are isolated by group name
- Group names are case-sensitive
- All group members must use the exact same group name
- Perfect for races, group rides, or team tracking

#### Group Horn
- Press "📢 Bocina Grupal" to send an audio alert to all group members
- Must be actively tracking to use
- Plays a distinctive horn sound on all devices in the group

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
1. Verify server URL in settings
2. Check internet connection
3. App auto-reconnects up to 10 times
4. Check browser console for errors

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
- `calculateBearing()` - Calculate direction between two GPS points

#### Map Management
- `initMap()` - Initialize Leaflet map instance
- `updateMap()` - Refresh user markers and tracks
- `createArrowIcon()` - Generate directional user markers
- `clearMapTracks()` - Remove trajectory polylines

#### WebSocket
- `connectWebSocket()` - Establish server connection
- Message types: `register`, `speed`, `users`, `group-horn`, `ping/pong`

#### UI State
- `updateConnectionStatus()` - Update WebSocket status indicator
- `updateGpsStatus()` - Update GPS status indicator
- `updateUsersList()` - Refresh user list display

### Browser Compatibility
- Chrome/Edge 90+
- Safari 14+
- Firefox 88+
- Mobile browsers with Geolocation API support

## Version History

- **v4.1** - Dark mode, fullscreen map, wake lock support
- **v4.0** - Decimal speed precision, group horn feature
- **v3.x** - User names in map markers, dark mode default
- Earlier versions - Core functionality

## License

This project is available for use under standard open source terms. Check with the repository owner for specific licensing details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Server

This client requires a compatible WebSocket server. See the server repository for setup instructions.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

**Note**: This application requires GPS access and an active internet connection to function properly. Accuracy depends on GPS signal quality and device capabilities.
