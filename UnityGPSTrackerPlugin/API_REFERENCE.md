# GPS Tracker Unity Plugin - API Reference

Complete API documentation for the GPS Tracker Unity Plugin.

## Table of Contents

- [GPSTrackerManager](#gpstrackermanager)
- [GPSUserData](#gpsuserdata)
- [GPSTrackerVisualizer](#gpstrackervisualizer)
- [GPSUtilities](#gpsutilities)
- [Events and Delegates](#events-and-delegates)
- [Enums](#enums)

---

## GPSTrackerManager

Singleton MonoBehaviour that manages the WebSocket connection to the GPS tracker server and handles real-time position updates.

### Accessing the Manager

```csharp
GPSTrackerManager manager = GPSTrackerManager.Instance;
```

The manager is automatically created as a singleton and persists across scenes (DontDestroyOnLoad).

### Properties

#### `ConnectionState` (GPSTrackerConnectionState)
```csharp
public GPSTrackerConnectionState ConnectionState { get; }
```
Current connection state. Read-only.

**Possible Values:**
- `Disconnected` - Not connected to server
- `Connecting` - Connection in progress
- `Connected` - Connected and ready
- `Error` - Connection error occurred

#### `IsConnected` (bool)
```csharp
public bool IsConnected { get; }
```
Returns `true` if connection state is `Connected`. Read-only.

#### `Users` (List<GPSUserData>)
```csharp
public List<GPSUserData> Users { get; }
```
List of all users currently in the group. Returns a copy of the internal list.

### Methods

#### `Connect()`
```csharp
public void Connect(
    string serverUrl = "wss://gps-tracker-server-production-5900.up.railway.app",
    string userId = "",
    string userName = "UnityUser",
    string groupName = "DefaultGroup"
)
```

Connect to the GPS tracker server.

**Parameters:**
- `serverUrl` - WebSocket server URL (must use `wss://` protocol)
- `userId` - Unique user identifier. If empty, auto-generates one
- `userName` - Display name shown to other users
- `groupName` - Group name to join. Only users in same group see each other

**Example:**
```csharp
GPSTrackerManager.Instance.Connect(
    userName: "Player1",
    groupName: "RaceGroup"
);
```

#### `Disconnect()`
```csharp
public void Disconnect()
```

Disconnect from the GPS tracker server. Cleans up WebSocket connection and clears user list.

**Example:**
```csharp
GPSTrackerManager.Instance.Disconnect();
```

#### `SendPositionUpdate()`
```csharp
public void SendPositionUpdate(
    double latitude,
    double longitude,
    double speed,
    float bearing,
    double maxSpeed
)
```

Send a position update to the server. Other users in the group will receive this update.

**Parameters:**
- `latitude` - GPS latitude (-90 to 90)
- `longitude` - GPS longitude (-180 to 180)
- `speed` - Current speed in km/h
- `bearing` - Current bearing in degrees (0-360, where 0 = North)
- `maxSpeed` - Maximum speed recorded in this session

**Example:**
```csharp
GPSTrackerManager.Instance.SendPositionUpdate(
    40.7128,   // New York latitude
    -74.0060,  // New York longitude
    50.0,      // 50 km/h
    90.0f,     // East
    100.0      // max speed
);
```

#### `SendGroupHorn()`
```csharp
public void SendGroupHorn()
```

Send a horn alert to all users in the group. Triggers `OnGroupHorn` event for all group members.

**Example:**
```csharp
GPSTrackerManager.Instance.SendGroupHorn();
```

#### `GetUserById()`
```csharp
public GPSUserData GetUserById(string userId)
```

Find a specific user by their ID.

**Parameters:**
- `userId` - User ID to search for

**Returns:**
- `GPSUserData` object if found, or `null` if not found

**Example:**
```csharp
GPSUserData user = GPSTrackerManager.Instance.GetUserById("user_123");
if (user != null)
{
    Debug.Log($"Found user: {user.userName}");
}
```

### Events

#### `OnConnectionStateChanged`
```csharp
public event Action<GPSTrackerConnectionState> OnConnectionStateChanged;
```

Fired when the connection state changes.

**Example:**
```csharp
GPSTrackerManager.Instance.OnConnectionStateChanged += state =>
{
    Debug.Log($"Connection state changed to: {state}");
};
```

#### `OnUsersUpdated`
```csharp
public event Action<List<GPSUserData>> OnUsersUpdated;
```

Fired when user position data is received from the server. Typically fires once per second per user.

**Example:**
```csharp
GPSTrackerManager.Instance.OnUsersUpdated += users =>
{
    Debug.Log($"Received {users.Count} users");
    foreach (var user in users)
    {
        Debug.Log($"  {user.userName}: {user.speed} km/h");
    }
};
```

#### `OnGroupHorn`
```csharp
public event Action OnGroupHorn;
```

Fired when a group horn alert is received from any user in the group.

**Example:**
```csharp
GPSTrackerManager.Instance.OnGroupHorn += () =>
{
    Debug.Log("Horn alert!");
    audioSource.PlayOneShot(hornSound);
};
```

#### `OnError`
```csharp
public event Action<string> OnError;
```

Fired when an error occurs (connection error, message parsing error, etc.).

**Example:**
```csharp
GPSTrackerManager.Instance.OnError += error =>
{
    Debug.LogError($"GPS Tracker error: {error}");
};
```

---

## GPSUserData

Data class representing a user's GPS position and information.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `userId` | string | Unique user identifier |
| `userName` | string | Display name |
| `speed` | double | Current speed in km/h |
| `latitude` | double | GPS latitude (-90 to 90) |
| `longitude` | double | GPS longitude (-180 to 180) |
| `bearing` | float | Bearing in degrees (0-360, 0 = North) |
| `timestamp` | long | Unix timestamp in milliseconds |
| `groupName` | string | Group name |

### Methods

#### `GetWorldPosition()`
```csharp
public Vector3 GetWorldPosition(float scale = 100000.0f)
```

Convert GPS coordinates to Unity world position using Mercator projection.

**Parameters:**
- `scale` - Scale factor for coordinate conversion (default: 100000)

**Returns:**
- `Vector3` world position (X = longitude, Z = latitude, Y = 0)

**Example:**
```csharp
GPSUserData user = /* ... */;
Vector3 worldPos = user.GetWorldPosition(100000f);
marker.transform.position = worldPos;
```

#### `GetRotation()`
```csharp
public Quaternion GetRotation()
```

Get Unity rotation from GPS bearing.

**Returns:**
- `Quaternion` rotation around Y-axis based on bearing

**Example:**
```csharp
GPSUserData user = /* ... */;
Quaternion rotation = user.GetRotation();
marker.transform.rotation = rotation;
```

#### `GetVelocityVector()`
```csharp
public Vector3 GetVelocityVector()
```

Get velocity vector based on speed and bearing.

**Returns:**
- `Vector3` velocity in meters/second in world space

**Example:**
```csharp
GPSUserData user = /* ... */;
Vector3 velocity = user.GetVelocityVector();
Debug.Log($"Moving at {velocity.magnitude} m/s");
```

#### `Clone()`
```csharp
public GPSUserData Clone()
```

Create a deep copy of this user data.

**Returns:**
- New `GPSUserData` instance with copied values

---

## GPSTrackerVisualizer

MonoBehaviour component that automatically visualizes GPS user positions in the 3D world.

### Inspector Properties

#### Coordinate Conversion

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `coordinateScale` | float | 100000 | Scale factor for GPS to Unity conversion |
| `markerHeightOffset` | float | 2.0 | Y-axis offset for markers |

#### Marker Appearance

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `markerSize` | float | 1.0 | Size of marker spheres |
| `showUserNames` | bool | true | Show user names above markers |
| `showSpeed` | bool | true | Show speed information |
| `defaultMarkerColor` | Color | Blue | Default marker color |

#### Movement Trails

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `drawTrails` | bool | true | Draw movement trails |
| `maxTrailPoints` | int | 100 | Max trail points per user |
| `trailColor` | Color | Cyan | Color of trails |
| `trailWidth` | float | 0.1 | Width of trail lines |

#### Dead Reckoning

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `enableDeadReckoning` | bool | true | - | Enable position prediction |
| `positionSmoothingFactor` | float | 0.15 | 0-1 | Interpolation smoothing |
| `maxExtrapolationTime` | float | 5.0 | 0-30 | Max prediction time (seconds) |
| `minSpeedForPrediction` | float | 1.0 | 0+ | Min speed for prediction (km/h) |
| `predictionDampingFactor` | float | 0.8 | 0-1 | Reduces prediction over time |

#### Interpolation Buffer

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `useInterpolationBuffer` | bool | false | - | Use buffer instead of dead reckoning |
| `interpolationBufferTime` | float | 0.2 | 0.05-2.0 | Buffer delay time (seconds) |
| `maxBufferSize` | int | 10 | 2-50 | Max buffered positions |

#### Prefabs

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `userMarkerPrefab` | GameObject | null | Custom marker prefab (optional) |

### Events

#### `OnUsersVisualizationUpdated`
```csharp
public event Action<List<GPSUserData>> OnUsersVisualizationUpdated;
```

Called when user visualizations are updated.

**Example:**
```csharp
visualizer.OnUsersVisualizationUpdated += users =>
{
    Debug.Log($"Visualizing {users.Count} users");
};
```

### Methods

#### `GetVisualizedUsers()` (Property)
```csharp
public List<GPSUserData> GetVisualizedUsers()
```

Get list of currently visualized users.

**Returns:**
- List of `GPSUserData` for all active visualized users

---

## GPSUtilities

Static utility class with helper functions for GPS coordinate conversion and calculations.

### Methods

#### `GPSToWorldPosition()`
```csharp
public static Vector3 GPSToWorldPosition(
    double latitude,
    double longitude,
    float scale = 100000.0f,
    float heightOffset = 0.0f
)
```

Convert GPS coordinates to Unity world position.

**Parameters:**
- `latitude` - GPS latitude
- `longitude` - GPS longitude
- `scale` - Scale factor (default: 100000)
- `heightOffset` - Y-axis offset (default: 0)

**Returns:**
- `Vector3` world position

**Example:**
```csharp
Vector3 pos = GPSUtilities.GPSToWorldPosition(40.7128, -74.0060, 100000f, 2.0f);
```

#### `WorldPositionToGPS()`
```csharp
public static void WorldPositionToGPS(
    Vector3 worldPosition,
    float scale,
    out double latitude,
    out double longitude
)
```

Convert Unity world position to GPS coordinates.

**Parameters:**
- `worldPosition` - Unity world position
- `scale` - Scale factor used for conversion
- `latitude` - Output latitude
- `longitude` - Output longitude

**Example:**
```csharp
GPSUtilities.WorldPositionToGPS(transform.position, 100000f, out double lat, out double lon);
Debug.Log($"Position: {lat}, {lon}");
```

#### `CalculateGPSDistance()`
```csharp
public static double CalculateGPSDistance(
    double lat1, double lon1,
    double lat2, double lon2
)
```

Calculate distance between two GPS coordinates using Haversine formula.

**Parameters:**
- `lat1`, `lon1` - First coordinate
- `lat2`, `lon2` - Second coordinate

**Returns:**
- Distance in kilometers

**Example:**
```csharp
double distance = GPSUtilities.CalculateGPSDistance(
    40.7128, -74.0060,  // New York
    34.0522, -118.2437  // Los Angeles
);
Debug.Log($"Distance: {distance} km");
```

#### `CalculateGPSBearing()`
```csharp
public static float CalculateGPSBearing(
    double lat1, double lon1,
    double lat2, double lon2
)
```

Calculate bearing between two GPS coordinates.

**Parameters:**
- `lat1`, `lon1` - Start coordinate
- `lat2`, `lon2` - End coordinate

**Returns:**
- Bearing in degrees (0-360, where 0 = North)

**Example:**
```csharp
float bearing = GPSUtilities.CalculateGPSBearing(
    40.7128, -74.0060,
    40.7589, -73.9851
);
Debug.Log($"Bearing: {bearing}° (North = 0°)");
```

#### `FormatSpeed()`
```csharp
public static string FormatSpeed(double speedKmh, bool showUnit = true)
```

Format speed for display.

**Parameters:**
- `speedKmh` - Speed in km/h
- `showUnit` - Whether to include "km/h" suffix

**Returns:**
- Formatted speed string

**Example:**
```csharp
string speed = GPSUtilities.FormatSpeed(65.7, true);
// Returns: "65.7 km/h"
```

#### `GetSpeedColor()`
```csharp
public static Color GetSpeedColor(double speedKmh, double maxSpeed = 100.0)
```

Get color based on speed (green = slow, red = fast).

**Parameters:**
- `speedKmh` - Current speed in km/h
- `maxSpeed` - Maximum speed for color scale

**Returns:**
- `Color` gradient from green (0) to yellow (50%) to red (100%)

**Example:**
```csharp
Color markerColor = GPSUtilities.GetSpeedColor(50.0, 100.0);
renderer.material.color = markerColor;
```

#### `GenerateUserId()`
```csharp
public static string GenerateUserId()
```

Generate a unique user ID based on device identifier and timestamp.

**Returns:**
- Unique user ID string

**Example:**
```csharp
string userId = GPSUtilities.GenerateUserId();
// Returns: "unity_[deviceId]_[timestamp]"
```

#### `GetCurrentTimestamp()`
```csharp
public static long GetCurrentTimestamp()
```

Get current Unix timestamp in milliseconds.

**Returns:**
- Unix timestamp (milliseconds since epoch)

**Example:**
```csharp
long timestamp = GPSUtilities.GetCurrentTimestamp();
```

---

## Events and Delegates

### Action Delegates

The plugin uses standard C# `Action` delegates for events:

```csharp
Action<GPSTrackerConnectionState>    // Connection state changed
Action<List<GPSUserData>>            // Users updated
Action                                // Group horn (no parameters)
Action<string>                        // Error (error message)
```

### Event Usage Pattern

```csharp
void Start()
{
    // Subscribe to events
    GPSTrackerManager.Instance.OnConnectionStateChanged += HandleConnectionChanged;
    GPSTrackerManager.Instance.OnUsersUpdated += HandleUsersUpdated;
}

void OnDestroy()
{
    // Unsubscribe from events
    if (GPSTrackerManager.Instance != null)
    {
        GPSTrackerManager.Instance.OnConnectionStateChanged -= HandleConnectionChanged;
        GPSTrackerManager.Instance.OnUsersUpdated -= HandleUsersUpdated;
    }
}

void HandleConnectionChanged(GPSTrackerConnectionState state)
{
    // Handle state change
}

void HandleUsersUpdated(List<GPSUserData> users)
{
    // Handle user updates
}
```

---

## Enums

### GPSTrackerConnectionState

Connection state enumeration.

```csharp
public enum GPSTrackerConnectionState
{
    Disconnected,  // Not connected
    Connecting,    // Connection in progress
    Connected,     // Connected and ready
    Error          // Connection error
}
```

**Usage:**
```csharp
if (GPSTrackerManager.Instance.ConnectionState == GPSTrackerConnectionState.Connected)
{
    Debug.Log("Ready to send position updates");
}
```

---

## Message Protocol

### WebSocket Message Format

All messages are JSON format.

#### Client → Server Messages

**Register Message:**
```json
{
  "type": "register",
  "userId": "unique_user_id",
  "userName": "Display Name",
  "groupName": "group_name"
}
```

**Position Update (Speed) Message:**
```json
{
  "type": "speed",
  "userId": "unique_user_id",
  "userName": "Display Name",
  "speed": 50.0,
  "latitude": 40.7128,
  "longitude": -74.0060,
  "bearing": 90.0,
  "timestamp": 1701234567890,
  "groupName": "group_name"
}
```

**Group Horn Message:**
```json
{
  "type": "group-horn",
  "userId": "unique_user_id",
  "groupName": "group_name"
}
```

**Pong Message (Keep-alive):**
```json
{
  "type": "pong"
}
```

#### Server → Client Messages

**Users Broadcast:**
```json
{
  "type": "users",
  "users": [
    {
      "userId": "user1",
      "userName": "Player 1",
      "speed": 50.0,
      "latitude": 40.7128,
      "longitude": -74.0060,
      "bearing": 90.0,
      "timestamp": 1701234567890,
      "groupName": "group_name"
    }
  ]
}
```

**Ping Message (Keep-alive):**
```json
{
  "type": "ping"
}
```

**Group Horn Broadcast:**
```json
{
  "type": "group-horn"
}
```

---

## Best Practices

### Connection Management

```csharp
// Connect in Start
void Start()
{
    GPSTrackerManager.Instance.Connect(
        userName: "Player1",
        groupName: "MyGroup"
    );
}

// Disconnect in OnDestroy or OnApplicationQuit
void OnDestroy()
{
    GPSTrackerManager.Instance.Disconnect();
}
```

### Error Handling

```csharp
void Start()
{
    GPSTrackerManager.Instance.OnError += error =>
    {
        Debug.LogError($"GPS Error: {error}");
        // Show UI notification
        // Attempt reconnect
    };
}
```

### Performance Optimization

```csharp
// Limit position update frequency
private float updateInterval = 1.0f;
private float nextUpdate = 0f;

void Update()
{
    if (Time.time >= nextUpdate)
    {
        SendPosition();
        nextUpdate = Time.time + updateInterval;
    }
}
```

### Memory Management

```csharp
// Always unsubscribe from events
void OnDestroy()
{
    if (GPSTrackerManager.Instance != null)
    {
        GPSTrackerManager.Instance.OnUsersUpdated -= HandleUsersUpdated;
    }
}
```

---

## Version History

- **v1.0.0** - Initial release
  - WebSocket connection management
  - Real-time position visualization
  - Dead reckoning and interpolation buffer
  - Comprehensive event system
  - GPS utilities

---

For more information, see:
- [README.md](README.md) - Overview and features
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [CHANGELOG.md](CHANGELOG.md) - Version history
