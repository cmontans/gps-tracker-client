# GPS Tracker Plugin - Complete API Reference

## Table of Contents

1. [Subsystem API](#subsystem-api)
2. [Data Structures](#data-structures)
3. [Actor API](#actor-api)
4. [Blueprint Library](#blueprint-library)
5. [Events and Delegates](#events-and-delegates)
6. [Enumerations](#enumerations)

---

## Subsystem API

### UGPSTrackerSubsystem

**Type:** `UGameInstanceSubsystem`

**Access:** `GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>()`

#### Public Functions

##### Connect
```cpp
void Connect(
    const FString& ServerURL = TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
    const FString& InUserId = TEXT(""),
    const FString& InUserName = TEXT("UnrealUser"),
    const FString& InGroupName = TEXT("DefaultGroup")
)
```
**Blueprint:** ✅ Callable

Connects to the GPS tracker server.

**Parameters:**
- `ServerURL` - WebSocket server URL (must start with `wss://`)
- `InUserId` - Unique user identifier (auto-generated if empty)
- `InUserName` - Display name for this user
- `InGroupName` - Group name to join (case-sensitive)

**Example:**
```cpp
Tracker->Connect(
    TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
    TEXT(""),
    TEXT("MyPlayer"),
    TEXT("RaceGroup")
);
```

---

##### Disconnect
```cpp
void Disconnect()
```
**Blueprint:** ✅ Callable

Disconnects from the GPS tracker server and cleans up resources.

---

##### SendPositionUpdate
```cpp
void SendPositionUpdate(
    double Latitude,
    double Longitude,
    double Speed,
    float Bearing,
    double MaxSpeed
)
```
**Blueprint:** ✅ Callable

Sends a position update to the server.

**Parameters:**
- `Latitude` - Current latitude in degrees
- `Longitude` - Current longitude in degrees
- `Speed` - Current speed in km/h
- `Bearing` - Direction in degrees (0-360, where 0 = North)
- `MaxSpeed` - Maximum speed recorded in the session

**Example:**
```cpp
Tracker->SendPositionUpdate(40.7128, -74.0060, 50.0, 90.0f, 120.0);
```

---

##### SendGroupHorn
```cpp
void SendGroupHorn()
```
**Blueprint:** ✅ Callable

Sends a horn alert to all users in the group.

---

##### GetConnectionState
```cpp
EGPSTrackerConnectionState GetConnectionState() const
```
**Blueprint:** ✅ Pure

Returns the current connection state.

**Returns:** `EGPSTrackerConnectionState` enum value

---

##### GetUsers
```cpp
TArray<FGPSUserData> GetUsers() const
```
**Blueprint:** ✅ Pure

Returns an array of all users currently in the group.

**Returns:** Array of `FGPSUserData` structures

---

##### GetUserById
```cpp
FGPSUserData GetUserById(const FString& UserId, bool& bFound) const
```
**Blueprint:** ✅ Pure

Finds a specific user by their ID.

**Parameters:**
- `UserId` - The user ID to search for
- `bFound` - Output parameter, true if user was found

**Returns:** `FGPSUserData` (empty if not found)

---

##### IsConnected
```cpp
bool IsConnected() const
```
**Blueprint:** ✅ Pure

Quick check if connected to the server.

**Returns:** `true` if connected, `false` otherwise

---

#### Public Properties

##### OnConnectionStateChanged
**Type:** `FOnConnectionStateChanged` (Multicast Delegate)

**Blueprint:** ✅ Assignable

Fired when the connection state changes.

**Signature:** `void OnConnectionStateChanged(EGPSTrackerConnectionState NewState)`

---

##### OnUsersUpdated
**Type:** `FOnUsersUpdated` (Multicast Delegate)

**Blueprint:** ✅ Assignable

Fired when user positions are updated from the server.

**Signature:** `void OnUsersUpdated(const TArray<FGPSUserData>& Users)`

---

##### OnGroupHorn
**Type:** `FOnGroupHorn` (Multicast Delegate)

**Blueprint:** ✅ Assignable

Fired when a group horn alert is received.

**Signature:** `void OnGroupHorn()`

---

##### OnError
**Type:** `FOnTrackerError` (Multicast Delegate)

**Blueprint:** ✅ Assignable

Fired when an error occurs.

**Signature:** `void OnError(const FString& ErrorMessage)`

---

## Data Structures

### FGPSUserData

**Type:** `USTRUCT(BlueprintType)`

Represents a user's position and information.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `UserId` | `FString` | Unique user identifier (UUID) |
| `UserName` | `FString` | Display name for the user |
| `Speed` | `double` | Current speed in km/h |
| `Latitude` | `double` | GPS latitude in degrees |
| `Longitude` | `double` | GPS longitude in degrees |
| `Bearing` | `float` | Direction in degrees (0-360) |
| `Timestamp` | `int64` | Unix timestamp in milliseconds |
| `GroupName` | `FString` | Group name the user belongs to |

#### Methods

##### GetWorldPosition
```cpp
FVector GetWorldPosition(float Scale = 100000.0f) const
```
Converts GPS coordinates to Unreal world position using Mercator projection.

**Parameters:**
- `Scale` - Conversion scale (default: 100000 = 1 degree = 100km)

**Returns:** `FVector` world position (X = Longitude, Y = Latitude, Z = 0)

---

##### GetRotation
```cpp
FRotator GetRotation() const
```
Converts GPS bearing to Unreal rotation.

**Returns:** `FRotator` with Yaw set to bearing

---

## Actor API

### AGPSTrackerVisualizerActor

**Type:** `AActor` (Blueprintable)

Automatically visualizes GPS tracker user positions in the world.

#### Public Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `CoordinateScale` | `float` | 100000.0 | GPS to world conversion scale |
| `MarkerHeightOffset` | `float` | 200.0 | Z-axis offset for markers |
| `MarkerSize` | `float` | 50.0 | Size of marker spheres |
| `bShowUserNames` | `bool` | `true` | Show user names above markers |
| `bShowSpeed` | `bool` | `true` | Show speed information |
| `TextSize` | `float` | 20.0 | Size of text labels |
| `DefaultMarkerColor` | `FLinearColor` | Blue | Default marker color |
| `bDrawTrails` | `bool` | `true` | Draw movement trails |
| `MaxTrailPoints` | `int32` | 100 | Maximum trail points per user |
| `bEnableDeadReckoning` | `bool` | `true` | Enable position prediction between GPS updates |
| `PositionSmoothingFactor` | `float` | 0.15 | Interpolation smoothing (0=instant, 1=very smooth) |
| `MaxExtrapolationTime` | `float` | 5.0 | Maximum seconds to extrapolate beyond last update |
| `MinSpeedForPrediction` | `float` | 1.0 | Minimum speed (km/h) to apply prediction |
| `PredictionDampingFactor` | `float` | 0.8 | Damping for prediction accuracy over time |
| `bUseInterpolationBuffer` | `bool` | `false` | Use interpolation buffer instead of dead reckoning |
| `InterpolationBufferTime` | `float` | 0.2 | Time delay for buffer interpolation (seconds) |
| `MaxBufferSize` | `int32` | 10 | Maximum number of buffered positions |

#### Public Functions

##### GetVisualizedUsers
```cpp
TArray<FGPSUserData> GetVisualizedUsers() const
```
**Blueprint:** ✅ Pure

Returns all currently visualized users.

#### Blueprint Events

##### OnUsersVisualizationUpdated
```cpp
UFUNCTION(BlueprintImplementableEvent)
void OnUsersVisualizationUpdated(const TArray<FGPSUserData>& Users)
```
Called when visualizations are updated. Implement in Blueprint to add custom logic.

---

##### CreateUserMarker
```cpp
UFUNCTION(BlueprintNativeEvent)
void CreateUserMarker(const FGPSUserData& UserData, const FVector& WorldLocation)
```
Called when a new user marker needs to be created. Override in Blueprint for custom markers.

**Parameters:**
- `UserData` - Information about the user
- `WorldLocation` - World position to spawn marker

---

## Blueprint Library

### UGPSTrackerBlueprintLibrary

**Type:** `UBlueprintFunctionLibrary`

Static helper functions for GPS tracking.

#### Functions

##### GetGPSTrackerSubsystem
```cpp
static UGPSTrackerSubsystem* GetGPSTrackerSubsystem(const UObject* WorldContextObject)
```
**Blueprint:** ✅ Pure (requires World Context)

Gets the GPS Tracker subsystem from world context.

**Returns:** Pointer to subsystem (may be null)

---

##### QuickConnect
```cpp
static void QuickConnect(
    const UObject* WorldContextObject,
    const FString& UserName,
    const FString& GroupName
)
```
**Blueprint:** ✅ Callable (requires World Context)

Quick connect with default server settings.

**Parameters:**
- `UserName` - Display name for the user
- `GroupName` - Group to join

---

##### GPSToWorldPosition
```cpp
static FVector GPSToWorldPosition(
    double Latitude,
    double Longitude,
    float Scale = 100000.0f,
    float HeightOffset = 0.0f
)
```
**Blueprint:** ✅ Pure

Converts GPS coordinates to Unreal world position.

**Returns:** `FVector` world position

---

##### WorldPositionToGPS
```cpp
static void WorldPositionToGPS(
    FVector WorldPosition,
    float Scale,
    double& OutLatitude,
    double& OutLongitude
)
```
**Blueprint:** ✅ Pure

Converts Unreal world position to GPS coordinates.

**Parameters:**
- `WorldPosition` - Position in Unreal space
- `Scale` - Conversion scale
- `OutLatitude` - Output latitude
- `OutLongitude` - Output longitude

---

##### CalculateGPSDistance
```cpp
static double CalculateGPSDistance(
    double Lat1,
    double Lon1,
    double Lat2,
    double Lon2
)
```
**Blueprint:** ✅ Pure

Calculates distance between two GPS coordinates using Haversine formula.

**Returns:** Distance in kilometers

**Example:**
```cpp
double DistanceKm = UGPSTrackerBlueprintLibrary::CalculateGPSDistance(
    40.7128, -74.0060,  // New York
    51.5074, -0.1278    // London
);
// Returns: ~5570 km
```

---

##### CalculateGPSBearing
```cpp
static float CalculateGPSBearing(
    double Lat1,
    double Lon1,
    double Lat2,
    double Lon2
)
```
**Blueprint:** ✅ Pure

Calculates bearing from point 1 to point 2.

**Returns:** Bearing in degrees (0-360)

---

##### FormatSpeed
```cpp
static FString FormatSpeed(double SpeedKmh, bool bShowUnit = true)
```
**Blueprint:** ✅ Pure

Formats speed value for display.

**Returns:** Formatted string (e.g., "50.0 km/h" or "50.0")

---

##### GetSpeedColor
```cpp
static FLinearColor GetSpeedColor(double SpeedKmh, double MaxSpeed = 200.0)
```
**Blueprint:** ✅ Pure

Gets color based on speed (blue = slow, green = medium, red = fast).

**Returns:** `FLinearColor` gradient from blue to red

---

## Events and Delegates

### Delegate Signatures

#### FOnConnectionStateChanged
```cpp
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(
    FOnConnectionStateChanged,
    EGPSTrackerConnectionState, NewState
);
```
Fired when connection state changes.

---

#### FOnUsersUpdated
```cpp
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(
    FOnUsersUpdated,
    const TArray<FGPSUserData>&, Users
);
```
Fired when user positions are updated.

---

#### FOnGroupHorn
```cpp
DECLARE_DYNAMIC_MULTICAST_DELEGATE(FOnGroupHorn);
```
Fired when group horn alert is received.

---

#### FOnTrackerError
```cpp
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(
    FOnTrackerError,
    const FString&, ErrorMessage
);
```
Fired when an error occurs.

---

## Enumerations

### EGPSTrackerConnectionState

**Type:** `UENUM(BlueprintType)`

Represents the current connection state.

| Value | Description |
|-------|-------------|
| `Disconnected` | Not connected to server |
| `Connecting` | Connection attempt in progress |
| `Connected` | Successfully connected |
| `Error` | Connection error occurred |

---

## Usage Patterns

### Pattern 1: Basic Connection
```cpp
UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();
Tracker->Connect(TEXT("wss://..."), TEXT(""), TEXT("Player1"), TEXT("GroupA"));
```

### Pattern 2: Event Binding
```cpp
Tracker->OnUsersUpdated.AddDynamic(this, &AMyClass::HandleUsers);
Tracker->OnConnectionStateChanged.AddDynamic(this, &AMyClass::HandleState);
```

### Pattern 3: Position Updates
```cpp
void Tick(float DeltaTime)
{
    if (Tracker->IsConnected())
    {
        Tracker->SendPositionUpdate(CurrentLat, CurrentLon, Speed, Bearing, MaxSpeed);
    }
}
```

### Pattern 4: Custom Visualization
```cpp
// Override CreateUserMarker in Blueprint
void CreateUserMarker(FGPSUserData UserData, FVector Location)
{
    // Spawn custom actor
    // Set properties
    // Add effects
}
```

---

## Constants

### Default Values

```cpp
const FString DEFAULT_SERVER_URL = TEXT("wss://gps-tracker-server-production-5900.up.railway.app");
const float DEFAULT_COORDINATE_SCALE = 100000.0f;
const float DEFAULT_MARKER_HEIGHT_OFFSET = 200.0f;
const float DEFAULT_MARKER_SIZE = 50.0f;
const float DEFAULT_TEXT_SIZE = 20.0f;
const int32 DEFAULT_MAX_TRAIL_POINTS = 100;
const FLinearColor DEFAULT_MARKER_COLOR = FLinearColor::Blue;
```

---

## Error Codes and Messages

### Common Error Messages

| Message | Cause | Solution |
|---------|-------|----------|
| "Failed to parse GPS Tracker message" | Invalid JSON | Check server message format |
| "GPS Tracker message missing type field" | Malformed message | Verify protocol version |
| "Cannot send position update: not connected" | Not connected | Call Connect() first |
| "Failed to load default sphere mesh" | Missing engine asset | Check Engine/BasicShapes content |

---

## Performance Considerations

### Recommended Limits

- **Users per group:** 50-100 for optimal performance
- **Trail points per user:** 100 (configurable)
- **Position update frequency:** 1 second (matches Android app)
- **WebSocket message size:** < 1KB per message

### Optimization Tips

1. Disable trails if not needed (`bDrawTrails = false`)
2. Reduce `MaxTrailPoints` for many users
3. Use LOD system for distant markers
4. Batch position updates when possible
5. Consider custom marker meshes instead of default sphere

---

**Last Updated:** December 2025
**Plugin Version:** 1.0.0
**Unreal Engine:** 5.3+
