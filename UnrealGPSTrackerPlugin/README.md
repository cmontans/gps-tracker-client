# GPS Tracker Unreal Engine Plugin

![Unreal Engine 5.3](https://img.shields.io/badge/Unreal%20Engine-5.3-blue)
![License](https://img.shields.io/badge/License-MIT-green)

A real-time GPS tracking visualization plugin for Unreal Engine 5.3 that connects to a WebSocket-based GPS tracker server and displays group member positions in your Unreal project.

## Features

- ✅ **Real-time WebSocket Connection** - Connects to GPS tracker server with automatic reconnection
- ✅ **Group-based Tracking** - View all users in your group simultaneously
- ✅ **3D Visualization** - Automatic spawning and updating of user position markers
- ✅ **Dead Reckoning** - Smooth position prediction between GPS updates using velocity and bearing
- ✅ **Blueprint Support** - Fully Blueprint-accessible API for easy integration
- ✅ **Customizable Visualization** - Configurable markers, colors, trails, and labels
- ✅ **Position Broadcasting** - Send your own position updates to the server
- ✅ **GPS Utilities** - Built-in functions for GPS coordinate conversion and calculations

## Installation

### Method 1: Copy to Project Plugins Folder

1. Copy the `UnrealGPSTrackerPlugin` folder to your project's `Plugins` directory:
   ```
   YourProject/
   └── Plugins/
       └── GPSTracker/
   ```

2. Right-click your project's `.uproject` file and select "Generate Visual Studio project files" (or equivalent for your IDE)

3. Open your project in Unreal Engine. The plugin should automatically load.

4. If prompted, enable the plugin in **Edit → Plugins → Project → Networking → GPS Tracker Integration**

### Method 2: Engine Plugins Folder (All Projects)

1. Copy the plugin to your Unreal Engine installation's Plugins folder:
   ```
   [UnrealEngineInstall]/Engine/Plugins/Marketplace/GPSTracker/
   ```

2. Restart Unreal Engine

3. Enable the plugin in your project: **Edit → Plugins → Installed → Networking → GPS Tracker Integration**

## Quick Start

### C++ Setup

1. Add the plugin to your project's `.Build.cs` file:

```csharp
PublicDependencyModuleNames.AddRange(new string[] {
    "Core",
    "CoreUObject",
    "Engine",
    "GPSTracker"  // Add this line
});
```

2. Include the subsystem in your code:

```cpp
#include "GPSTrackerSubsystem.h"
#include "GPSTrackerTypes.h"

// Get the subsystem
UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();

// Connect to server
Tracker->Connect(
    TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
    TEXT(""),  // Auto-generate user ID
    TEXT("MyUserName"),
    TEXT("MyGroupName")
);

// Bind to events
Tracker->OnUsersUpdated.AddDynamic(this, &YourClass::HandleUsersUpdated);
```

### Blueprint Setup

1. **Connect to Server:**
   - Call `Get GPS Tracker Subsystem` (from any world context)
   - Call `Connect` with your server URL, user name, and group name

2. **Visualize Positions:**
   - Place a `GPS Tracker Visualizer Actor` in your level
   - Configure visualization settings in the Details panel
   - The actor will automatically display all group members when connected

3. **Quick Connect (Shortcut):**
   - Use `Quick Connect` from the Blueprint library for one-line connection with default settings

![Blueprint Example](Docs/blueprint_example.png)

## Usage Examples

### Example 1: Simple Connection and Event Handling

**Blueprint:**

```
Event BeginPlay
  → Get GPS Tracker Subsystem
  → Connect (URL: "wss://...", UserName: "Player1", GroupName: "RaceGroup")
  → Bind Event to On Users Updated

On Users Updated
  → For Each User
    → Print User Name and Speed
```

**C++:**

```cpp
void AMyGameMode::BeginPlay()
{
    Super::BeginPlay();

    UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();
    Tracker->OnUsersUpdated.AddDynamic(this, &AMyGameMode::HandleUsersUpdated);
    Tracker->Connect(TEXT("wss://..."), TEXT(""), TEXT("Player1"), TEXT("RaceGroup"));
}

void AMyGameMode::HandleUsersUpdated(const TArray<FGPSUserData>& Users)
{
    for (const FGPSUserData& User : Users)
    {
        UE_LOG(LogTemp, Log, TEXT("User: %s, Speed: %.1f km/h"), *User.UserName, User.Speed);
    }
}
```

### Example 2: Send Position Updates

If you want to send your Unreal character's position to the GPS tracker server:

```cpp
void AMyCharacter::Tick(float DeltaTime)
{
    Super::Tick(DeltaTime);

    // Get current position
    FVector WorldPos = GetActorLocation();

    // Convert to GPS coordinates (example scale)
    double Latitude, Longitude;
    UGPSTrackerBlueprintLibrary::WorldPositionToGPS(WorldPos, 100000.0f, Latitude, Longitude);

    // Get speed and bearing
    FVector Velocity = GetVelocity();
    double SpeedKmh = Velocity.Size() * 0.036f; // Convert cm/s to km/h
    float Bearing = GetActorRotation().Yaw;

    // Send update
    UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();
    Tracker->SendPositionUpdate(Latitude, Longitude, SpeedKmh, Bearing, MaxSpeedThisSession);
}
```

### Example 3: Custom Marker Visualization

Create a Blueprint derived from `GPS Tracker Visualizer Actor` and override the `Create User Marker` event:

```
Create User Marker Event
  → Spawn Actor from Class (YourCustomMarkerActor)
  → Set Actor Location (World Location input)
  → Set Actor Text (User Data → User Name)
  → Add custom effects, particles, etc.
```

## API Reference

### UGPSTrackerSubsystem

The main subsystem for GPS tracking. Access via `GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>()`.

#### Functions

| Function | Description |
|----------|-------------|
| `Connect(ServerURL, UserId, UserName, GroupName)` | Connect to GPS tracker server |
| `Disconnect()` | Disconnect from server |
| `SendPositionUpdate(Lat, Lon, Speed, Bearing, MaxSpeed)` | Send position update to server |
| `SendGroupHorn()` | Send horn alert to all group members |
| `GetConnectionState()` | Get current connection state |
| `GetUsers()` | Get array of all users in group |
| `GetUserById(UserId, bFound)` | Find specific user by ID |
| `IsConnected()` | Check if connected to server |

#### Events

| Event | Description |
|-------|-------------|
| `OnConnectionStateChanged` | Fired when connection state changes |
| `OnUsersUpdated` | Fired when user positions are updated |
| `OnGroupHorn` | Fired when group horn alert is received |
| `OnError` | Fired when an error occurs |

### FGPSUserData

Struct containing user position and information.

| Property | Type | Description |
|----------|------|-------------|
| `UserId` | FString | Unique user identifier |
| `UserName` | FString | Display name |
| `Speed` | double | Current speed in km/h |
| `Latitude` | double | GPS latitude |
| `Longitude` | double | GPS longitude |
| `Bearing` | float | Bearing in degrees (0-360) |
| `Timestamp` | int64 | Unix timestamp (milliseconds) |
| `GroupName` | FString | Group name |

### AGPSTrackerVisualizerActor

Actor that automatically visualizes user positions.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `CoordinateScale` | float | Scale for GPS to world conversion (default: 100000) |
| `MarkerHeightOffset` | float | Z-axis offset for markers (default: 200) |
| `MarkerSize` | float | Size of marker spheres (default: 50) |
| `bShowUserNames` | bool | Show user names above markers |
| `bShowSpeed` | bool | Show speed information |
| `TextSize` | float | Size of text labels (default: 20) |
| `DefaultMarkerColor` | FLinearColor | Default marker color |
| `bDrawTrails` | bool | Draw movement trails |
| `MaxTrailPoints` | int32 | Maximum trail points per user (default: 100) |
| `bEnableDeadReckoning` | bool | Enable position prediction between GPS updates (default: true) |
| `PositionSmoothingFactor` | float | Smoothing for position interpolation, 0-1 (default: 0.15) |
| `MaxExtrapolationTime` | float | Max seconds to predict beyond last update (default: 5.0) |
| `MinSpeedForPrediction` | float | Min speed (km/h) to apply prediction (default: 1.0) |
| `PredictionDampingFactor` | float | Reduces prediction over time, 0-1 (default: 0.8) |
| `bUseInterpolationBuffer` | bool | Use interpolation buffer instead of dead reckoning (default: false) |
| `InterpolationBufferTime` | float | Time delay for buffer interpolation in seconds (default: 0.2) |
| `MaxBufferSize` | int32 | Maximum buffered positions (default: 10) |

#### Events

| Event | Description |
|-------|-------------|
| `OnUsersVisualizationUpdated` | Called when visualizations are updated |
| `CreateUserMarker` | Override to customize marker creation |

### UGPSTrackerBlueprintLibrary

Static helper functions for GPS tracking.

| Function | Description |
|----------|-------------|
| `GetGPSTrackerSubsystem(WorldContext)` | Get subsystem from world context |
| `QuickConnect(WorldContext, UserName, GroupName)` | Quick connect with defaults |
| `GPSToWorldPosition(Lat, Lon, Scale, HeightOffset)` | Convert GPS to Unreal coordinates |
| `WorldPositionToGPS(WorldPos, Scale, OutLat, OutLon)` | Convert Unreal to GPS coordinates |
| `CalculateGPSDistance(Lat1, Lon1, Lat2, Lon2)` | Calculate distance in km (Haversine) |
| `CalculateGPSBearing(Lat1, Lon1, Lat2, Lon2)` | Calculate bearing between points |
| `FormatSpeed(SpeedKmh, bShowUnit)` | Format speed for display |
| `GetSpeedColor(SpeedKmh, MaxSpeed)` | Get color based on speed |

## Configuration

### Server URL

Default server: `wss://gps-tracker-server-production-5900.up.railway.app`

You can specify a custom server URL when calling `Connect()`.

### Coordinate Scaling

The plugin uses a simple Mercator projection to convert GPS coordinates to Unreal world space:

- **Default Scale**: 100000 (1 degree = 100km in Unreal units)
- **Formula**: `WorldX = Longitude * Scale`, `WorldY = Latitude * Scale`

Adjust the scale based on your game's world size and GPS coordinate range.

### Dead Reckoning

Dead reckoning provides smooth, continuous movement between GPS position updates by predicting positions based on the last known velocity and bearing.

**How It Works:**
1. **Interpolation**: Smoothly moves markers toward GPS positions using configurable smoothing
2. **Prediction**: Extrapolates position forward using velocity vector from speed and bearing
3. **Damping**: Reduces prediction accuracy over time to prevent overshooting
4. **Threshold**: Only predicts when speed exceeds minimum threshold

**Configuration Parameters:**
- `bEnableDeadReckoning` - Enable/disable dead reckoning (default: true)
- `PositionSmoothingFactor` - Controls interpolation speed (lower = smoother, 0.15 default)
- `MaxExtrapolationTime` - Stop predicting after this many seconds (5.0 default)
- `MinSpeedForPrediction` - Only predict above this speed in km/h (1.0 default)
- `PredictionDampingFactor` - Reduces extrapolation over time (0.8 default)

**Benefits:**
- ✅ Eliminates jerky movement from GPS updates (typically 1 second intervals)
- ✅ Creates natural-looking motion paths
- ✅ Compensates for GPS update latency
- ✅ Prevents markers from "teleporting" between positions

**Tuning Tips:**
- Increase `PositionSmoothingFactor` for more responsive (less smooth) movement
- Decrease `MaxExtrapolationTime` if predictions overshoot actual paths
- Increase `MinSpeedForPrediction` to disable prediction for stationary/slow users
- Adjust `PredictionDampingFactor` to control how aggressively predictions decay

### Interpolation Buffer

Interpolation buffer provides an alternative smoothing method that interpolates between buffered past positions for guaranteed accuracy at the cost of slight latency.

**How It Works:**
1. **Buffering**: Stores recent GPS positions with timestamps in a circular buffer
2. **Delayed Rendering**: Renders positions from the past (e.g., 200ms ago)
3. **Interpolation**: Smoothly interpolates between buffered positions
4. **Accuracy**: Always accurate since it uses actual GPS data, never predictions

**Configuration Parameters:**
- `bUseInterpolationBuffer` - Use interpolation buffer instead of dead reckoning (default: false)
- `InterpolationBufferTime` - Time delay in seconds for rendering (0.2 default)
- `MaxBufferSize` - Maximum positions to buffer (10 default)

**Benefits:**
- ✅ Guaranteed accuracy - uses only actual GPS data
- ✅ Smooth movement even with jittery GPS updates
- ✅ No overshooting - never predicts beyond known positions
- ✅ Handles irregular update rates well

**Trade-offs:**
- ⚠️ Adds latency (positions rendered ~200ms behind real-time)
- ⚠️ Requires sufficient GPS update frequency
- ⚠️ Less responsive than dead reckoning

**Comparison: Dead Reckoning vs Interpolation Buffer**

| Feature | Dead Reckoning | Interpolation Buffer |
|---------|----------------|---------------------|
| **Latency** | None (real-time) | ~200ms delay |
| **Accuracy** | Can overshoot | Always accurate |
| **Responsiveness** | Very responsive | Slightly delayed |
| **Smoothness** | Smooth with prediction | Smooth with buffering |
| **Best For** | Real-time tracking, racing | Replay analysis, recording |

**Tuning Tips:**
- Use 100-200ms buffer time for good balance between smoothness and latency
- Increase buffer size (15-20) for very irregular GPS update rates
- Decrease buffer time (50-100ms) for more responsive visualization
- Use dead reckoning for live racing, interpolation buffer for replay/analysis

### Message Protocol

The plugin implements the GPS tracker WebSocket protocol:

**Client → Server:**
- `register`: Join group with user info
- `speed`: Send position update
- `group-horn`: Send horn alert
- `pong`: Keep-alive response

**Server → Client:**
- `users`: Broadcast of all group member positions
- `ping`: Keep-alive request
- `group-horn`: Horn alert notification

## Troubleshooting

### Plugin Not Loading

1. Ensure you're using Unreal Engine 5.3 or compatible version
2. Regenerate project files
3. Check Output Log for error messages
4. Verify plugin is enabled in Edit → Plugins

### Connection Fails

1. Check server URL is correct (must start with `wss://`)
2. Verify internet connection
3. Check firewall settings for WebSocket traffic
4. Look for error messages in Output Log
5. Ensure server is running and accessible

### No Markers Visible

1. Ensure `GPSTrackerVisualizerActor` is placed in the level
2. Check that subsystem is connected (`IsConnected()`)
3. Verify `OnUsersUpdated` event is firing
4. Check coordinate scale matches your GPS data range
5. Ensure markers aren't spawning outside camera view

### Performance Issues

1. Reduce `MaxTrailPoints` if drawing many trails
2. Disable `bDrawTrails` for better performance
3. Limit number of users in group
4. Consider LOD system for distant markers

## Architecture

```
GPSTrackerSubsystem (GameInstanceSubsystem)
  ├─ WebSocket Connection (IWebSocket)
  ├─ Message Parsing (JSON)
  ├─ User Data Management (TArray<FGPSUserData>)
  └─ Event Broadcasting (Delegates)

GPSTrackerVisualizerActor
  ├─ User Marker Components
  │  ├─ StaticMeshComponent (sphere)
  │  ├─ TextRenderComponent (name)
  │  └─ TextRenderComponent (speed)
  └─ Trail Visualization (DrawDebugLine)

GPSTrackerBlueprintLibrary
  └─ Static Helper Functions
```

## Server Protocol

For server implementation details, see the GPS Tracker Server repository.

**WebSocket Endpoint**: `wss://[server]/`

**Message Format**: JSON

## Contributing

Contributions are welcome! Please submit pull requests or issues on the project repository.

## License

MIT License - See LICENSE file for details

## Support

For issues, questions, or feature requests, please contact the development team or submit an issue on GitHub.

## Credits

Developed by GPS Tracker Team
Compatible with Unreal Engine 5.3+

---

**Version**: 1.0.0
**Last Updated**: December 2025
