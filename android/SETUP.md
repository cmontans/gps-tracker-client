# Quick Setup Guide

## Prerequisites

1. **Android Studio**: Download from https://developer.android.com/studio
2. **Google Maps API Key**: Get from https://console.cloud.google.com/

## Setup Steps

### 1. Get Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable "Maps SDK for Android" API
4. Go to Credentials → Create Credentials → API Key
5. (Recommended) Restrict the key:
   - Application restrictions: Android apps
   - Add package name: `com.tracker.gps`
   - Add SHA-1 fingerprint (from debug/release keystore)

### 2. Configure API Key

Edit `app/src/main/AndroidManifest.xml` line 28:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual API key.

### 3. Build and Run

#### Using Android Studio:
1. Open Android Studio
2. File → Open → Select the `android` directory
3. Wait for Gradle sync to complete
4. Connect Android device or start emulator
5. Run → Run 'app' (or press Shift+F10)

#### Using Command Line:

**Linux/macOS:**
```bash
cd android

# Build the app
./gradlew build

# Install on connected device
./gradlew installDebug

# Or build release APK
./gradlew assembleRelease
# APK will be in app/build/outputs/apk/release/
```

**Windows:**
```cmd
cd android

# Build the app
gradlew.bat build

# Install on connected device
gradlew.bat installDebug

# Or build release APK
gradlew.bat assembleRelease
REM APK will be in app\build\outputs\apk\release\
```

## First Run

1. Launch the app
2. Grant location permission when prompted
3. Grant notification permission (Android 13+)
4. Enter your name and group name
5. Tap "Start Tracking"
6. Go outside for best GPS signal

## Troubleshooting

### Map not showing
- Verify API key is correct
- Check API key restrictions
- Ensure "Maps SDK for Android" is enabled
- Check package name matches: `com.tracker.gps`

### GPS not working
- Grant location permission
- Enable Location Services on device
- Go outside for better signal
- Wait 10-30 seconds for GPS lock

### Build errors
- Ensure Android Studio is up to date
- Sync Gradle files (File → Sync Project with Gradle Files)
- Clean and rebuild (Build → Clean Project, then Build → Rebuild Project)
- Check you have the required SDK components

## Default Server

The app connects to: `wss://gps-tracker-server-production-5900.up.railway.app`

To use a different server:
1. Open Settings in the app
2. Change Server URL
3. Tap OK

## Support

See [README.md](README.md) for full documentation.

For issues: https://github.com/cmontans/gps-tracker-client/issues
