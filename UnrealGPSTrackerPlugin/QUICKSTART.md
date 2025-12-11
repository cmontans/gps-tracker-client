# GPS Tracker Plugin - Quick Start Guide

This guide will help you get started with the GPS Tracker plugin in 5 minutes.

## Step 1: Install the Plugin

1. Copy the `GPSTracker` plugin folder to your project's `Plugins` directory
2. Restart Unreal Editor
3. Enable the plugin: **Edit → Plugins → Search "GPS Tracker" → Check the box**
4. Restart the editor when prompted

## Step 2: Create a Simple Test Level

### Option A: Blueprint-Only Setup (Recommended for Quick Testing)

1. **Open or create a new level**

2. **Add the Visualizer Actor:**
   - Open **Content Browser**
   - Click **Add → Blueprint Class → Actor**
   - Search for `GPSTrackerVisualizerActor` as parent class
   - Name it `BP_GPSVisualizer`
   - Drag `BP_GPSVisualizer` into your level

3. **Create a Level Blueprint connection script:**
   - Open **Blueprints → Open Level Blueprint**
   - Create this node graph:

   ```
   Event BeginPlay
     ↓
   Quick Connect
     • User Name: "TestUser"
     • Group Name: "UnrealTestGroup"
   ```

4. **Play and Test:**
   - Press **Play** (Alt+P)
   - Open the Output Log (**Window → Developer Tools → Output Log**)
   - Look for: `"Connected to GPS Tracker Server"`

### Option B: C++ Setup

1. **Add to your .Build.cs file:**
   ```csharp
   PublicDependencyModuleNames.AddRange(new string[] {
       "Core", "CoreUObject", "Engine", "InputCore",
       "GPSTracker"  // Add this
   });
   ```

2. **In your GameMode or PlayerController BeginPlay:**
   ```cpp
   #include "GPSTrackerSubsystem.h"

   void AMyGameMode::BeginPlay()
   {
       Super::BeginPlay();

       UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();
       if (Tracker)
       {
           Tracker->Connect(
               TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
               TEXT(""),
               TEXT("UnrealUser"),
               TEXT("UnrealTestGroup")
           );

           Tracker->OnUsersUpdated.AddDynamic(this, &AMyGameMode::OnUsersUpdated);
       }
   }

   void AMyGameMode::OnUsersUpdated(const TArray<FGPSUserData>& Users)
   {
       UE_LOG(LogTemp, Log, TEXT("Received %d users"), Users.Num());
   }
   ```

## Step 3: Test with Real GPS Data

To see the visualization in action, you'll need GPS data from the group. You have two options:

### Option 1: Use the Android App

1. Install the GPS Tracker Android app on your phone
2. Open the app and enter the same group name: `"UnrealTestGroup"`
3. Start tracking on your phone
4. Your position should appear in Unreal Engine!

### Option 2: Send Test Position Data

Create a Blueprint or C++ function to send test data:

**Blueprint:**
```
Delay 2.0 seconds
  ↓
Get GPS Tracker Subsystem
  ↓
Send Position Update
  • Latitude: 40.7128 (New York example)
  • Longitude: -74.0060
  • Speed: 50.0
  • Bearing: 90.0
  • Max Speed: 60.0
```

**C++:**
```cpp
// Send a test position every second
GetWorld()->GetTimerManager().SetTimer(
    TestTimerHandle,
    [this]()
    {
        UGPSTrackerSubsystem* Tracker = GetGameInstance()->GetSubsystem<UGPSTrackerSubsystem>();
        if (Tracker && Tracker->IsConnected())
        {
            // Example: New York coordinates
            Tracker->SendPositionUpdate(
                40.7128,  // Latitude
                -74.0060, // Longitude
                50.0,     // Speed (km/h)
                90.0f,    // Bearing (East)
                60.0      // Max speed
            );
        }
    },
    1.0f,
    true
);
```

## Step 4: Customize Visualization

Select the `GPSTrackerVisualizerActor` in your level and adjust these properties:

| Property | Effect |
|----------|--------|
| **Coordinate Scale** | Adjust if markers are too far/close (default: 100000) |
| **Marker Height Offset** | Height above ground (default: 200 units) |
| **Marker Size** | Size of user spheres (default: 50) |
| **Show User Names** | Display names above markers |
| **Show Speed** | Display speed info |
| **Draw Trails** | Show movement trails |
| **Default Marker Color** | Color of user markers |

## Step 5: Handle Events

### Blueprint Event Handling

Add these events to your Level Blueprint or custom Blueprint:

```
Get GPS Tracker Subsystem
  ↓
Bind Event to On Users Updated
  ↓
On Users Updated (Event)
  ↓
For Each Loop (Users array)
  ↓
Print String: "User: {UserName}, Speed: {Speed} km/h"
```

### Common Events to Bind:

1. **On Connection State Changed**
   - Know when connected/disconnected
   - Update UI accordingly

2. **On Users Updated**
   - Process new position data
   - Update your own game logic

3. **On Group Horn**
   - React to horn alerts
   - Play sounds, show notifications

4. **On Error**
   - Handle connection errors
   - Display error messages to user

## Complete Blueprint Example

Here's a complete Level Blueprint setup:

```
[Event BeginPlay]
  ↓
[Quick Connect]
  User Name: "UnrealPlayer"
  Group Name: "TestGroup"
  ↓
[Get GPS Tracker Subsystem]
  ↓
[Bind Event to On Connection State Changed]
  ↓
  [On Connection State Changed Event]
    ↓
  [Switch on EGPSTrackerConnectionState]
    ├─ Connected → [Print String: "✓ Connected!"]
    ├─ Disconnected → [Print String: "✗ Disconnected"]
    ├─ Connecting → [Print String: "⋯ Connecting..."]
    └─ Error → [Print String: "⚠ Connection Error"]

[Separate Event Graph]
[Get GPS Tracker Subsystem]
  ↓
[Bind Event to On Users Updated]
  ↓
  [On Users Updated Event]
    ↓
  [Get Length (Users array)]
    ↓
  [Print String: "Active users: {Length}"]
    ↓
  [For Each Loop]
    ↓
  [Print String: "{UserName}: {Speed} km/h at ({Latitude}, {Longitude})"]
```

## Troubleshooting Quick Fixes

### "Cannot connect to server"
- Check internet connection
- Verify server URL is correct
- Check Output Log for detailed error

### "No markers appearing"
- Verify `GPSTrackerVisualizerActor` is in level
- Check if connected: call `Is Connected` node
- Verify coordinate scale (try different values: 1000, 10000, 100000)
- Ensure GPS data is being received (check On Users Updated event)

### "Markers too far apart or clustered"
- Adjust `Coordinate Scale` on the Visualizer Actor
- Lower value = markers closer together
- Higher value = markers farther apart

### "Can't find plugin nodes in Blueprint"
- Verify plugin is enabled in Edit → Plugins
- Restart Unreal Editor
- Check that you're searching for "GPS Tracker" in Blueprint palette

## Next Steps

1. **Customize Markers**: Override `Create User Marker` event in a Blueprint child of `GPSTrackerVisualizerActor`
2. **Add UI**: Create a widget to show connection status and user list
3. **Integrate with Gameplay**: Use GPS positions to drive game mechanics
4. **Add Audio**: Play sounds on position updates or horn alerts
5. **Extend Protocol**: Add custom message types for your game

## Advanced: Custom Marker Blueprint

1. Create a new Blueprint based on `GPSTrackerVisualizerActor`
2. Override the `Create User Marker` event
3. Add your custom visuals:

```
[Create User Marker Event]
  User Data (input)
  World Location (input)
  ↓
[Spawn Actor from Class: BP_CustomMarker]
  Location: World Location
  ↓
[Cast to BP_CustomMarker]
  ↓
[Set User Name Text: UserData.UserName]
[Set Speed Text: UserData.Speed]
[Set Marker Color: Get Speed Color (UserData.Speed)]
```

## Example Use Cases

- **Racing Game**: Show real-world race participants in-game
- **Delivery Simulation**: Track delivery vehicles in real-time
- **Multiplayer Training**: Coordinate with real-world team members
- **Live Events**: Display attendee positions at large venues
- **Fleet Management**: Visualize vehicle fleets in 3D space

## Support

For more details, see `README.md` in the plugin directory.

Happy tracking! 🚀
