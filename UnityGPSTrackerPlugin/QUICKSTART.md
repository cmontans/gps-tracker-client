# GPS Tracker Plugin - Quick Start Guide

This guide will help you get started with the GPS Tracker Unity plugin in 5 minutes.

## Step 1: Install the Plugin

### Install NativeWebSocket Library

1. Open Unity Package Manager: **Window → Package Manager**
2. Click **+ → Add package from git URL...**
3. Enter: `https://github.com/endel/NativeWebSocket.git#upm`
4. Click **Add**

### Install GPS Tracker Plugin

**Option A: Via Package Manager**
1. In Package Manager, click **+ → Add package from disk...**
2. Navigate to `UnityGPSTrackerPlugin` folder and select `package.json`

**Option B: Copy to Assets**
1. Copy the `UnityGPSTrackerPlugin` folder to `Assets/`
2. Unity will auto-import

## Step 2: Create a Simple Test Scene

### Quick Setup (Recommended)

1. **Create Connection Script:**

Create a new C# script called `GPSQuickStart.cs`:

```csharp
using GPSTracker;
using UnityEngine;

public class GPSQuickStart : MonoBehaviour
{
    void Start()
    {
        // Connect to server
        GPSTrackerManager.Instance.Connect(
            userName: "TestUser",
            groupName: "UnityTestGroup"
        );

        // Subscribe to events
        GPSTrackerManager.Instance.OnConnectionStateChanged += OnConnectionChanged;
        GPSTrackerManager.Instance.OnUsersUpdated += OnUsersUpdated;
    }

    void OnConnectionChanged(GPSTrackerConnectionState state)
    {
        Debug.Log($"GPS Tracker: {state}");
    }

    void OnUsersUpdated(System.Collections.Generic.List<GPSUserData> users)
    {
        Debug.Log($"GPS Tracker: Received {users.Count} users");
        foreach (var user in users)
        {
            Debug.Log($"  - {user.userName}: {user.speed:F1} km/h");
        }
    }
}
```

2. **Create GameObject:**
   - **GameObject → Create Empty** (name it "GPS Setup")
   - Attach the `GPSQuickStart.cs` script

3. **Add Visualizer:**
   - **GameObject → Create Empty** (name it "GPS Visualizer")
   - **Add Component → GPS Tracker Visualizer**
   - Configure in Inspector:
     - Coordinate Scale: 100000
     - Marker Height Offset: 2
     - Show User Names: ✓
     - Show Speed: ✓
     - Draw Trails: ✓

4. **Play and Test:**
   - Press **Play**
   - Check Console for: `"GPS Tracker: Connected"`
   - You should see user markers appear when GPS data is received

## Step 3: Test with Real GPS Data

To see the visualization in action, you need GPS data from the group.

### Option 1: Use the Android App

1. Install the GPS Tracker Android app on your phone
2. Open the app and enter group name: `"UnityTestGroup"`
3. Start tracking on your phone
4. Your position should appear in Unity!

### Option 2: Send Test Position Data

Add this method to your `GPSQuickStart.cs` script:

```csharp
using System.Collections;

public class GPSQuickStart : MonoBehaviour
{
    void Start()
    {
        // ... existing code ...

        // Send test positions after 2 seconds
        StartCoroutine(SendTestPositions());
    }

    IEnumerator SendTestPositions()
    {
        yield return new WaitForSeconds(2f);

        // Wait for connection
        while (!GPSTrackerManager.Instance.IsConnected)
        {
            yield return new WaitForSeconds(0.5f);
        }

        Debug.Log("Sending test position updates...");

        // Simulate movement
        double lat = 40.7128; // New York coordinates
        double lon = -74.0060;
        float bearing = 0f;

        while (true)
        {
            // Move north and increase bearing
            lat += 0.0001;
            bearing = (bearing + 10f) % 360f;

            GPSTrackerManager.Instance.SendPositionUpdate(
                lat,
                lon,
                50.0,  // 50 km/h
                bearing,
                60.0   // max speed
            );

            yield return new WaitForSeconds(1f);
        }
    }

    // ... rest of code ...
}
```

## Step 4: Customize Visualization

Select the **GPS Visualizer** GameObject and adjust these Inspector properties:

### Coordinate Conversion
| Property | Effect | Default |
|----------|--------|---------|
| **Coordinate Scale** | Adjust if markers are too far/close | 100000 |
| **Marker Height Offset** | Height above ground (Y axis) | 2 |

### Marker Appearance
| Property | Effect | Default |
|----------|--------|---------|
| **Marker Size** | Size of user spheres | 1 |
| **Show User Names** | Display names above markers | ✓ |
| **Show Speed** | Display speed info | ✓ |
| **Default Marker Color** | Color of user markers | Blue |

### Movement Trails
| Property | Effect | Default |
|----------|--------|---------|
| **Draw Trails** | Show movement trails | ✓ |
| **Max Trail Points** | Points per trail | 100 |
| **Trail Color** | Color of trails | Cyan |
| **Trail Width** | Width of trail lines | 0.1 |

### Dead Reckoning (Smoothing)
| Property | Effect | Default |
|----------|--------|---------|
| **Enable Dead Reckoning** | Smooth movement prediction | ✓ |
| **Position Smoothing Factor** | Smoothing amount (0-1) | 0.15 |
| **Max Extrapolation Time** | Max prediction seconds | 5.0 |
| **Min Speed For Prediction** | Min speed to predict (km/h) | 1.0 |

## Step 5: Handle Events

### Connection State Events

```csharp
void Start()
{
    GPSTrackerManager.Instance.OnConnectionStateChanged += state =>
    {
        switch (state)
        {
            case GPSTrackerConnectionState.Connected:
                Debug.Log("✓ Connected!");
                break;
            case GPSTrackerConnectionState.Disconnected:
                Debug.Log("✗ Disconnected");
                break;
            case GPSTrackerConnectionState.Connecting:
                Debug.Log("⋯ Connecting...");
                break;
            case GPSTrackerConnectionState.Error:
                Debug.Log("⚠ Connection Error");
                break;
        }
    };
}
```

### User Updates Events

```csharp
void Start()
{
    GPSTrackerManager.Instance.OnUsersUpdated += users =>
    {
        Debug.Log($"Active users: {users.Count}");

        foreach (var user in users)
        {
            string info = $"{user.userName}: {user.speed:F1} km/h at ({user.latitude:F6}, {user.longitude:F6})";
            Debug.Log(info);
        }
    };
}
```

### Group Horn Events

```csharp
void Start()
{
    GPSTrackerManager.Instance.OnGroupHorn += () =>
    {
        Debug.Log("📢 Group horn received!");
        // Play sound, show notification, etc.
    };
}
```

### Error Events

```csharp
void Start()
{
    GPSTrackerManager.Instance.OnError += error =>
    {
        Debug.LogError($"GPS Tracker Error: {error}");
    };
}
```

## Complete Example Script

Here's a complete example combining all features:

```csharp
using GPSTracker;
using UnityEngine;
using System.Collections.Generic;

public class CompleteGPSExample : MonoBehaviour
{
    void Start()
    {
        // Get manager instance
        var manager = GPSTrackerManager.Instance;

        // Subscribe to all events
        manager.OnConnectionStateChanged += OnConnectionChanged;
        manager.OnUsersUpdated += OnUsersUpdated;
        manager.OnGroupHorn += OnGroupHorn;
        manager.OnError += OnError;

        // Connect to server
        manager.Connect(
            userName: "UnityPlayer",
            groupName: "TestGroup"
        );
    }

    void OnConnectionChanged(GPSTrackerConnectionState state)
    {
        Debug.Log($"Connection: {state}");
    }

    void OnUsersUpdated(List<GPSUserData> users)
    {
        Debug.Log($"Users updated: {users.Count} active");
    }

    void OnGroupHorn()
    {
        Debug.Log("Horn alert received!");
    }

    void OnError(string error)
    {
        Debug.LogError($"Error: {error}");
    }

    // Optional: Send position updates
    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space))
        {
            GPSTrackerManager.Instance.SendGroupHorn();
        }
    }
}
```

## Troubleshooting Quick Fixes

### "Cannot connect to server"
- ✓ Check internet connection
- ✓ Verify server URL is correct
- ✓ Check Console for detailed error
- ✓ Ensure NativeWebSocket is installed

### "No markers appearing"
- ✓ Verify `GPSTrackerVisualizer` component is in scene
- ✓ Check if connected: `GPSTrackerManager.Instance.IsConnected`
- ✓ Verify coordinate scale (try different values: 1000, 10000, 100000)
- ✓ Ensure GPS data is being received (check `OnUsersUpdated` event)
- ✓ Check camera can see the marker positions

### "Markers too far apart or clustered"
- ✓ Adjust `Coordinate Scale` on the Visualizer component
- ✓ Lower value = markers closer together
- ✓ Higher value = markers farther apart
- ✓ Use camera distance to guide scale selection

### "NativeWebSocket errors"
- ✓ Ensure NativeWebSocket is installed via Package Manager
- ✓ Check for version conflicts
- ✓ Try reinstalling NativeWebSocket package

### "Markers are jerky/jumping"
- ✓ Enable Dead Reckoning in Visualizer
- ✓ Adjust Position Smoothing Factor (0.15 is good starting point)
- ✓ Or try Interpolation Buffer for guaranteed accuracy

## Next Steps

1. **Custom Markers**: Assign a custom prefab to `User Marker Prefab` in the Visualizer
2. **Add UI**: Create Canvas with connection status and user list
3. **Integrate with Gameplay**: Use GPS positions to drive game mechanics
4. **Add Audio**: Play sounds on position updates or horn alerts
5. **Extend Protocol**: Add custom message types for your game

## Advanced: Custom Marker Prefab

1. **Create a prefab** for your custom marker:
   - Create GameObject with your custom visuals
   - Add any components you need
   - Save as prefab

2. **Assign to Visualizer:**
   - Drag prefab to `User Marker Prefab` field in GPS Visualizer component

3. **Access user data:**
   - Subscribe to `GPSTrackerVisualizer.OnUsersVisualizationUpdated` event
   - Update your custom visuals based on `GPSUserData`

Example:
```csharp
public class CustomMarkerController : MonoBehaviour
{
    public void SetUserData(GPSUserData userData)
    {
        // Update custom visuals
        GetComponent<Renderer>().material.color =
            GPSUtilities.GetSpeedColor(userData.speed);

        // Update custom UI
        transform.Find("NameLabel").GetComponent<TextMesh>().text =
            userData.userName;
    }
}
```

## Example Use Cases

- **Racing Game**: Show real-world race participants in-game
- **Delivery Simulation**: Track delivery vehicles in real-time
- **Multiplayer Training**: Coordinate with real-world team members
- **Live Events**: Display attendee positions at large venues
- **Fleet Management**: Visualize vehicle fleets in 3D space
- **AR Applications**: Overlay real GPS positions in augmented reality

## Support

For more details, see `README.md` and `API_REFERENCE.md` in the plugin directory.

Happy tracking! 🚀
