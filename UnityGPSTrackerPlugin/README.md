# GPS Tracker Unity Plugin

![Unity](https://img.shields.io/badge/Unity-2020.3+-blue)
![License](https://img.shields.io/badge/License-MIT-green)

A real-time GPS tracking visualization plugin for Unity that connects to a WebSocket-based GPS tracker server and displays group member positions in your Unity project.

## Features

- ✅ **Real-time WebSocket Connection** - Connects to GPS tracker server with automatic reconnection
- ✅ **Group-based Tracking** - View all users in your group simultaneously
- ✅ **3D Visualization** - Automatic spawning and updating of user position markers
- ✅ **Dead Reckoning** - Smooth position prediction between GPS updates using velocity and bearing
- ✅ **Interpolation Buffer** - Alternative smoothing method with guaranteed accuracy
- ✅ **Easy Integration** - Simple singleton manager pattern, drop-in components
- ✅ **Customizable Visualization** - Configurable markers, colors, trails, and labels
- ✅ **Position Broadcasting** - Send your own position updates to the server
- ✅ **GPS Utilities** - Built-in functions for GPS coordinate conversion and calculations

## Installation

### Method 1: Unity Package Manager (Recommended)

1. Open Unity Package Manager (**Window → Package Manager**)
2. Click **+ → Add package from disk...**
3. Navigate to the `UnityGPSTrackerPlugin` folder and select `package.json`
4. The plugin will be imported and ready to use

### Method 2: Copy to Assets Folder

1. Copy the `UnityGPSTrackerPlugin` folder to your Unity project's `Assets` folder:
   ```
   YourProject/
   └── Assets/
       └── UnityGPSTrackerPlugin/
   ```

2. Unity will automatically import the plugin

### Dependencies

The plugin requires the **NativeWebSocket** library for WebSocket communication:

1. Install via Package Manager:
   - Add from git URL: `https://github.com/endel/NativeWebSocket.git#upm`
   - Or download and add manually from [NativeWebSocket GitHub](https://github.com/endel/NativeWebSocket)

## Quick Start

### C# Setup

1. **Create a GPS Tracker Manager in your scene:**

```csharp
using GPSTracker;
using UnityEngine;

public class GPSSetup : MonoBehaviour
{
    void Start()
    {
        // Get the singleton manager instance
        GPSTrackerManager manager = GPSTrackerManager.Instance;

        // Subscribe to events
        manager.OnConnectionStateChanged += OnConnectionChanged;
        manager.OnUsersUpdated += OnUsersUpdated;

        // Connect to server
        manager.Connect(
            "wss://gps-tracker-server-production-5900.up.railway.app",
            "",  // Auto-generate user ID
            "UnityUser",
            "MyGroupName"
        );
    }

    void OnConnectionChanged(GPSTrackerConnectionState state)
    {
        Debug.Log($"Connection state: {state}");
    }

    void OnUsersUpdated(List<GPSUserData> users)
    {
        Debug.Log($"Received {users.Count} users");
    }
}
```

2. **Add the Visualizer component:**

- Create an empty GameObject in your scene
- Add the `GPSTrackerVisualizer` component
- Configure visualization settings in the Inspector
- The visualizer will automatically display all group members when connected

### Unity Inspector Setup

1. **Create a GPS Manager:**
   - The manager is created automatically as a singleton when first accessed
   - It persists across scene changes (DontDestroyOnLoad)

2. **Add Visualizer:**
   - Create Empty GameObject: **GameObject → Create Empty** (name it "GPS Visualizer")
   - Add Component: **Add Component → GPS Tracker Visualizer**
   - Configure settings in Inspector (coordinate scale, marker size, colors, etc.)

3. **Create Connection Script:**
   - Create a new C# script to handle connection
   - Attach to a GameObject in your scene
   - Call `GPSTrackerManager.Instance.Connect()` in `Start()`

## Usage Examples

### Example 1: Simple Connection and Visualization

```csharp
using GPSTracker;
using UnityEngine;

public class SimpleGPSDemo : MonoBehaviour
{
    void Start()
    {
        // Connect to server
        GPSTrackerManager.Instance.Connect(
            userName: "Player1",
            groupName: "RaceGroup"
        );

        // Subscribe to user updates
        GPSTrackerManager.Instance.OnUsersUpdated += HandleUsersUpdated;
    }

    void HandleUsersUpdated(List<GPSUserData> users)
    {
        foreach (var user in users)
        {
            Debug.Log($"User: {user.userName}, Speed: {user.speed:F1} km/h");
        }
    }
}
```

### Example 2: Send Position Updates

If you want to send your Unity character's position to the GPS tracker server:

```csharp
using GPSTracker;
using UnityEngine;

public class PositionBroadcaster : MonoBehaviour
{
    public Transform characterTransform;
    private float updateInterval = 1.0f;
    private float nextUpdateTime = 0f;

    void Update()
    {
        if (Time.time >= nextUpdateTime)
        {
            SendPositionUpdate();
            nextUpdateTime = Time.time + updateInterval;
        }
    }

    void SendPositionUpdate()
    {
        // Get current position
        Vector3 worldPos = characterTransform.position;

        // Convert to GPS coordinates
        GPSUtilities.WorldPositionToGPS(worldPos, 100000.0f, out double latitude, out double longitude);

        // Get speed and bearing
        Vector3 velocity = GetComponent<Rigidbody>()?.velocity ?? Vector3.zero;
        double speedKmh = velocity.magnitude * 3.6f; // m/s to km/h
        float bearing = Mathf.Atan2(velocity.x, velocity.z) * Mathf.Rad2Deg;
        if (bearing < 0) bearing += 360;

        // Send update
        GPSTrackerManager.Instance.SendPositionUpdate(
            latitude, longitude, speedKmh, bearing, 100.0
        );
    }
}
```

### Example 3: Custom Marker Visualization

```csharp
using GPSTracker;
using UnityEngine;
using System.Collections.Generic;

public class CustomGPSVisualizer : MonoBehaviour
{
    public GameObject customMarkerPrefab;
    private Dictionary<string, GameObject> customMarkers = new Dictionary<string, GameObject>();

    void Start()
    {
        GPSTrackerManager.Instance.OnUsersUpdated += UpdateCustomMarkers;
    }

    void UpdateCustomMarkers(List<GPSUserData> users)
    {
        foreach (var user in users)
        {
            if (!customMarkers.ContainsKey(user.userId))
            {
                // Create custom marker
                Vector3 worldPos = user.GetWorldPosition(100000f);
                GameObject marker = Instantiate(customMarkerPrefab, worldPos, user.GetRotation());
                customMarkers[user.userId] = marker;

                // Customize appearance based on speed
                Color speedColor = GPSUtilities.GetSpeedColor(user.speed);
                marker.GetComponent<Renderer>().material.color = speedColor;
            }
            else
            {
                // Update existing marker
                GameObject marker = customMarkers[user.userId];
                marker.transform.position = user.GetWorldPosition(100000f);
                marker.transform.rotation = user.GetRotation();
            }
        }
    }
}
```

## API Reference

### GPSTrackerManager

Singleton manager for GPS tracking. Access via `GPSTrackerManager.Instance`.

#### Methods

| Method | Description |
|--------|-------------|
| `Connect(serverUrl, userId, userName, groupName)` | Connect to GPS tracker server |
| `Disconnect()` | Disconnect from server |
| `SendPositionUpdate(lat, lon, speed, bearing, maxSpeed)` | Send position update to server |
| `SendGroupHorn()` | Send horn alert to all group members |
| `GetUserById(userId)` | Find specific user by ID |

#### Properties

| Property | Description |
|----------|-------------|
| `ConnectionState` | Current connection state (enum) |
| `IsConnected` | Boolean indicating if connected |
| `Users` | List of all users in group |

#### Events

| Event | Description |
|-------|-------------|
| `OnConnectionStateChanged` | Fired when connection state changes |
| `OnUsersUpdated` | Fired when user positions are updated |
| `OnGroupHorn` | Fired when group horn alert is received |
| `OnError` | Fired when an error occurs |

### GPSUserData

Class containing user position and information.

| Property | Type | Description |
|----------|------|-------------|
| `userId` | string | Unique user identifier |
| `userName` | string | Display name |
| `speed` | double | Current speed in km/h |
| `latitude` | double | GPS latitude |
| `longitude` | double | GPS longitude |
| `bearing` | float | Bearing in degrees (0-360) |
| `timestamp` | long | Unix timestamp (milliseconds) |
| `groupName` | string | Group name |

#### Methods

| Method | Description |
|--------|-------------|
| `GetWorldPosition(scale)` | Convert GPS to Unity world position |
| `GetRotation()` | Get Unity rotation from bearing |
| `GetVelocityVector()` | Get velocity vector from speed and bearing |

### GPSTrackerVisualizer

MonoBehaviour that automatically visualizes user positions.

#### Inspector Properties

| Property | Type | Description |
|----------|------|-------------|
| `coordinateScale` | float | Scale for GPS to world conversion (default: 100000) |
| `markerHeightOffset` | float | Y-axis offset for markers (default: 2) |
| `markerSize` | float | Size of marker spheres (default: 1) |
| `showUserNames` | bool | Show user names above markers |
| `showSpeed` | bool | Show speed information |
| `defaultMarkerColor` | Color | Default marker color |
| `drawTrails` | bool | Draw movement trails |
| `maxTrailPoints` | int | Maximum trail points per user (default: 100) |
| `enableDeadReckoning` | bool | Enable position prediction (default: true) |
| `positionSmoothingFactor` | float | Smoothing for interpolation, 0-1 (default: 0.15) |
| `maxExtrapolationTime` | float | Max seconds to predict beyond last update (default: 5.0) |
| `minSpeedForPrediction` | float | Min speed (km/h) to apply prediction (default: 1.0) |
| `predictionDampingFactor` | float | Reduces prediction over time, 0-1 (default: 0.8) |
| `useInterpolationBuffer` | bool | Use interpolation buffer instead of dead reckoning |
| `interpolationBufferTime` | float | Time delay for buffer interpolation (default: 0.2) |
| `maxBufferSize` | int | Maximum buffered positions (default: 10) |
| `userMarkerPrefab` | GameObject | Custom prefab for markers (optional) |

#### Events

| Event | Description |
|-------|-------------|
| `OnUsersVisualizationUpdated` | Called when visualizations are updated |

### GPSUtilities

Static helper functions for GPS tracking.

| Method | Description |
|--------|-------------|
| `GPSToWorldPosition(lat, lon, scale, heightOffset)` | Convert GPS to Unity coordinates |
| `WorldPositionToGPS(worldPos, scale, out lat, out lon)` | Convert Unity to GPS coordinates |
| `CalculateGPSDistance(lat1, lon1, lat2, lon2)` | Calculate distance in km (Haversine) |
| `CalculateGPSBearing(lat1, lon1, lat2, lon2)` | Calculate bearing between points |
| `FormatSpeed(speedKmh, showUnit)` | Format speed for display |
| `GetSpeedColor(speedKmh, maxSpeed)` | Get color based on speed |
| `GenerateUserId()` | Generate unique user ID |
| `GetCurrentTimestamp()` | Get current Unix timestamp |

## Configuration

### Server URL

Default server: `wss://gps-tracker-server-production-5900.up.railway.app`

You can specify a custom server URL when calling `Connect()`.

### Coordinate Scaling

The plugin uses a simple Mercator projection to convert GPS coordinates to Unity world space:

- **Default Scale**: 100000 (1 degree = 100km in Unity units)
- **Formula**: `WorldX = Longitude * Scale`, `WorldZ = Latitude * Scale`

Adjust the scale based on your game's world size and GPS coordinate range.

### Dead Reckoning

Dead reckoning provides smooth, continuous movement between GPS position updates by predicting positions based on the last known velocity and bearing.

**How It Works:**
1. **Interpolation**: Smoothly moves markers toward GPS positions using configurable smoothing
2. **Prediction**: Extrapolates position forward using velocity vector from speed and bearing
3. **Damping**: Reduces prediction accuracy over time to prevent overshooting
4. **Threshold**: Only predicts when speed exceeds minimum threshold

**Configuration Parameters:**
- `enableDeadReckoning` - Enable/disable dead reckoning (default: true)
- `positionSmoothingFactor` - Controls interpolation speed (lower = smoother, 0.15 default)
- `maxExtrapolationTime` - Stop predicting after this many seconds (5.0 default)
- `minSpeedForPrediction` - Only predict above this speed in km/h (1.0 default)
- `predictionDampingFactor` - Reduces extrapolation over time (0.8 default)

**Benefits:**
- ✅ Eliminates jerky movement from GPS updates (typically 1 second intervals)
- ✅ Creates natural-looking motion paths
- ✅ Compensates for GPS update latency
- ✅ Prevents markers from "teleporting" between positions

### Interpolation Buffer

Interpolation buffer provides an alternative smoothing method that interpolates between buffered past positions.

**Configuration Parameters:**
- `useInterpolationBuffer` - Use interpolation buffer instead of dead reckoning (default: false)
- `interpolationBufferTime` - Time delay in seconds for rendering (0.2 default)
- `maxBufferSize` - Maximum positions to buffer (10 default)

**Comparison: Dead Reckoning vs Interpolation Buffer**

| Feature | Dead Reckoning | Interpolation Buffer |
|---------|----------------|---------------------|
| **Latency** | None (real-time) | ~200ms delay |
| **Accuracy** | Can overshoot | Always accurate |
| **Responsiveness** | Very responsive | Slightly delayed |
| **Smoothness** | Smooth with prediction | Smooth with buffering |
| **Best For** | Real-time tracking, racing | Replay analysis, recording |

## Troubleshooting

### Connection Fails

1. Check server URL is correct (must start with `wss://`)
2. Verify internet connection
3. Ensure NativeWebSocket library is installed
4. Check Console for error messages
5. Ensure server is running and accessible

### No Markers Visible

1. Ensure `GPSTrackerVisualizer` component is in the scene
2. Check that manager is connected (`GPSTrackerManager.Instance.IsConnected`)
3. Verify users are being received (subscribe to `OnUsersUpdated` event)
4. Check coordinate scale matches your GPS data range
5. Ensure markers aren't spawning outside camera view

### Performance Issues

1. Reduce `maxTrailPoints` if drawing many trails
2. Disable `drawTrails` for better performance
3. Limit number of users in group
4. Use object pooling for markers

## Architecture

```
GPSTrackerManager (Singleton MonoBehaviour)
  ├─ WebSocket Connection (NativeWebSocket)
  ├─ Message Parsing (JSON)
  ├─ User Data Management (List<GPSUserData>)
  └─ Event Broadcasting (C# Events)

GPSTrackerVisualizer (MonoBehaviour)
  ├─ User Marker GameObjects
  │  ├─ Sphere Mesh (or custom prefab)
  │  ├─ TextMesh (name)
  │  └─ TextMesh (speed)
  └─ Trail Visualization (LineRenderer)

GPSUtilities (Static Class)
  └─ Helper Functions
```

## Server Protocol

**WebSocket Endpoint**: `wss://[server]/`

**Message Format**: JSON

**Client → Server:**
- `register`: Join group with user info
- `speed`: Send position update
- `group-horn`: Send horn alert
- `pong`: Keep-alive response

**Server → Client:**
- `users`: Broadcast of all group member positions
- `ping`: Keep-alive request
- `group-horn`: Horn alert notification

## Contributing

Contributions are welcome! Please submit pull requests or issues on the project repository.

## License

MIT License - See LICENSE file for details

## Support

For issues, questions, or feature requests, please contact the development team or submit an issue on GitHub.

## Credits

Developed by GPS Tracker Team
Compatible with Unity 2020.3+

---

**Version**: 1.0.0
**Last Updated**: December 2025
