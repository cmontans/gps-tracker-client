# Garmin GPS Tracker - Connect IQ App

This is the Garmin Connect IQ implementation of the GPS Tracker app for Garmin smartwatches and fitness devices.

## Overview

The GPS Tracker Connect IQ app allows Garmin device users to track their GPS speed and sync with the group tracking server. It supports both standalone tracking and companion mode with the Android phone app.

## Supported Devices

The app is compatible with most modern Garmin devices, including:

### Fenix Series
- Fenix 7 / 7S / 7X
- Fenix 6 / 6S / 6 Pro
- Fenix 5 / 5S / 5X / 5 Plus

### Forerunner Series
- Forerunner 955, 945, 935
- Forerunner 745, 645, 645 Music
- Forerunner 255 / 255 Music / 255S

### Vivoactive Series
- Vivoactive 5, 4 / 4S
- Vivoactive 3 / 3 Music / 3 Music LTE

### Venu Series
- Venu 3 / 3S
- Venu 2 / 2 Plus / 2S
- Venu

### Epix Series
- Epix Gen 2
- Epix Pro (42mm, 47mm, 51mm)

### Other Series
- Instinct 2 / 2S
- MARQ series (Adventurer, Athlete, Aviator, Captain, Commander, Driver, Expedition, Golfer)
- Approach S62, S70

## Features

### Display
- **Large Speed Display**: Current speed in km/h with easy-to-read font
- **Statistics**: Max speed and average speed tracking
- **Status Indicators**: GPS signal quality and phone connection status
- **Tracking Indicator**: Visual indicator when tracking is active

### Controls
- **Start/Stop**: Toggle tracking with SELECT button
- **Menu**: Access options via MENU button
  - Start/Stop tracking
  - Reset statistics
  - Send group horn
  - Settings

### Data Field
- Add GPS speed to any watch face or activity screen
- Shows current speed with automatic updates
- Works with any Connect IQ compatible watch face

### Communication
- **Phone Sync**: Syncs data with Android app via Garmin Connect Mobile
- **Standalone Mode**: Works independently without phone
- **Server Sync**: Direct communication with tracking server (when available)

## Project Structure

```
garmin/
├── manifest.xml                    # App manifest and device support
├── monkey.jungle                   # Project configuration
├── source/                        # Monkey C source code
│   ├── GPSTrackerApp.mc          # Main application entry
│   ├── GPSTrackerView.mc         # Main UI view
│   ├── GPSTrackerDelegate.mc     # Input handling
│   ├── GPSManager.mc             # GPS tracking logic
│   ├── StatsManager.mc           # Statistics calculation
│   ├── CommunicationManager.mc   # Phone/server communication
│   └── SpeedDataField.mc         # Data field for watch faces
└── resources/                     # Resources
    ├── strings/                   # Localized strings
    ├── settings/                  # App settings
    ├── drawables/                 # Icons and images
    ├── fonts/                     # Custom fonts (if any)
    ├── layouts/                   # UI layouts
    └── menus/                     # Menu definitions
```

## Building

### Prerequisites

1. **Garmin Connect IQ SDK**
   - Download from [Garmin Developer Portal](https://developer.garmin.com/connect-iq/sdk/)
   - Install SDK and set up environment variables

2. **Visual Studio Code** (recommended)
   - Install "Monkey C" extension by Garmin

3. **Garmin Device or Simulator**
   - Use Connect IQ Simulator for testing
   - Or use a physical Garmin device

### Build Steps

#### Using Command Line:

```bash
# Navigate to project directory
cd garmin/

# Build for all supported devices
monkeyc -o gps-tracker.prg \
        -f monkey.jungle \
        -y /path/to/developer_key

# Build for specific device (e.g., Fenix 7)
monkeyc -o gps-tracker-fenix7.prg \
        -f monkey.jungle \
        -d fenix7 \
        -y /path/to/developer_key
```

#### Using Visual Studio Code:

1. Open the `garmin/` folder in VS Code
2. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
3. Type "Monkey C: Build for Device"
4. Select your target device
5. The `.prg` file will be generated in the `bin/` directory

### Generating Developer Key

```bash
# Generate a new developer key
openssl genrsa -out developer_key 4096
```

## Installation

### On Garmin Simulator:

```bash
# Launch simulator
connectiq

# Load app in simulator
monkeydo gps-tracker.prg fenix7
```

### On Physical Device:

1. **Via Garmin Express** (Easiest):
   - Open Garmin Express on your computer
   - Connect your Garmin device via USB
   - Copy `.prg` file to the device's `GARMIN/APPS/` folder
   - Disconnect device - app will appear in apps menu

2. **Via Connect IQ Store** (After publishing):
   - Open Connect IQ Store app on your Garmin device
   - Search for "GPS Tracker"
   - Download and install

3. **Via Sideload** (Development):
   ```bash
   # Using monkeydo command
   monkeydo gps-tracker.prg fenix7 -t
   ```

## Configuration

### App Settings

Access settings via: Menu → Settings

- **Server URL**: WebSocket server URL (default: production server)
- **User Name**: Display name for tracking
- **Group Name**: Group to join
- **Auto Start**: Automatically start tracking on app launch
- **Phone Sync**: Enable syncing with phone app

### Watch Face Data Field

To add speed to your watch face:

1. Long press on current watch face
2. Select "Customize"
3. Navigate to data fields
4. Choose "GPS Tracker - Speed"
5. Save changes

## Usage

### Starting Tracking

1. Open GPS Tracker app from apps menu
2. Wait for GPS signal (indicator turns green)
3. Press SELECT button to start tracking
4. View current speed on main screen

### Menu Options

Press MENU button to access:
- **Start/Stop**: Toggle tracking
- **Reset**: Reset max and average speed
- **Send Horn**: Notify your group
- **Settings**: Configure app

### Stopping Tracking

- Press SELECT button while tracking, OR
- Press MENU and select "Stop", OR
- Press BACK button (with confirmation)

## Phone App Integration

The Garmin app can communicate with the Android phone app:

### Setup

1. Install Android GPS Tracker app on phone
2. Install Garmin Connect Mobile app
3. Pair your Garmin device with phone
4. Open GPS Tracker on watch
5. Data will automatically sync

### Synced Data

**Watch → Phone:**
- Tracking start/stop commands
- Statistics reset requests
- Group horn notifications

**Phone → Watch:**
- Speed updates from phone GPS
- User list updates
- Connection status
- GPS status

### Benefits of Phone Sync

- More accurate GPS (can use phone GPS)
- Better battery life (phone handles server communication)
- Full user list with map view on phone
- Reliable WebSocket connection

## Standalone Mode

The app works independently without a phone:

1. Ensure "Phone Sync" is disabled in settings
2. Configure server URL in settings
3. Start tracking - watch will use own GPS
4. Limited to speed tracking (no user list)

## Development

### Monkey C Language

Connect IQ apps are written in Monkey C, Garmin's proprietary language similar to Java/C.

Key concepts:
- Object-oriented with classes and inheritance
- Strong typing with type inference
- Memory-constrained (optimize for small devices)
- No garbage collection (manual memory management)

### Adding Features

To add new features:

1. Modify source files in `source/` directory
2. Update strings in `resources/strings/strings.xml`
3. Rebuild with `monkeyc` command
4. Test in simulator before device deployment

### Debugging

```bash
# Run with debug output
monkeydo gps-tracker.prg fenix7 --debug

# View logs in console
# Use System.println() in code for logging
```

### Testing

- Use Connect IQ Simulator for initial testing
- Test on physical device for real-world scenarios
- Verify GPS accuracy outdoors
- Test battery impact during long sessions
- Check phone sync reliability

## Troubleshooting

### GPS Not Working
- Move outdoors with clear sky view
- Wait 1-2 minutes for GPS acquisition
- Check device GPS settings
- Try restarting the watch

### Phone Sync Not Working
- Ensure Garmin Connect Mobile is running on phone
- Check Bluetooth connection
- Verify phone app is also running
- Try toggling "Phone Sync" in settings

### App Crashes
- Check Connect IQ version compatibility
- Update watch firmware if needed
- Verify sufficient free memory on device
- Check error logs in Garmin Express

### Slow Performance
- Disable phone sync if not needed
- Reduce update frequency in settings
- Close other running apps on watch
- Restart watch

## Publishing to Connect IQ Store

1. **Register** as Garmin developer: https://developer.garmin.com/
2. **Test** thoroughly on multiple devices
3. **Create** store listing with:
   - App description
   - Screenshots
   - Icon (60x60 or 80x80 PNG)
   - Supported devices list
4. **Submit** for review via Garmin Developer Portal
5. **Wait** for approval (typically 3-5 business days)

## License

Same as main GPS Tracker project.

## Support

For issues specific to Garmin version:
- Check Garmin Developer Forums
- Review Connect IQ documentation
- Test with latest SDK version

For general GPS Tracker issues:
- See main project documentation
- Check server connection status
- Verify WebSocket URL

## Contributing

Contributions welcome! Areas for improvement:

- Additional language translations
- UI enhancements for smaller screens
- Battery optimization
- Additional statistics
- Activity integration
- Training plans

## Resources

- [Connect IQ Developer Portal](https://developer.garmin.com/connect-iq/)
- [Monkey C API Documentation](https://developer.garmin.com/connect-iq/api-docs/)
- [Connect IQ Forums](https://forums.garmin.com/developer/connect-iq/)
- [Connect IQ Store](https://apps.garmin.com/en-US/)

## Version History

### 1.0.0 (Current)
- Initial release
- Basic speed tracking
- Statistics (max, average)
- Phone sync support
- Data field for watch faces
- Multi-device support
- Standalone and companion modes
