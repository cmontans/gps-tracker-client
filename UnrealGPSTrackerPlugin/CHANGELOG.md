# Changelog

All notable changes to the GPS Tracker Unreal Plugin will be documented in this file.

## [1.3.0] - 2025-12-11

### Added
- **Cesium for Unreal Integration** - Optional support for accurate geospatial positioning
  - Automatic detection of CesiumForUnreal plugin
  - WGS84 coordinate transformation using Cesium's georeference system
  - Accurate world-scale positioning for global GPS tracking
  - Optional plugin dependency (works with or without Cesium)
- **Ground Clamping System** - Terrain-aware marker positioning
  - Automatic terrain height sampling using line traces
  - Configurable height offset above terrain
  - Prevents markers from clipping through 3D Tiles terrain
  - Sphere trace fallback for complex terrain geometry
- Cesium configuration properties in `AGPSTrackerVisualizerActor`:
  - `bUseCesiumGeoreference` - Enable Cesium coordinate transformation (default: false)
  - `bEnableGroundClamping` - Clamp markers to terrain height (default: false)
  - `GroundClampingOffset` - Height above terrain in Unreal units (default: 100.0)
- Automatic fallback to Mercator projection when Cesium unavailable
- Compile-time feature detection with WITH_CESIUM preprocessor flag
- Cached Cesium georeference reference for performance

### Improved
- Coordinate conversion now supports both simple Mercator and Cesium georeference
- More accurate positioning for large geographic areas with Cesium
- Better terrain integration for realistic marker placement
- Warning system for missing Cesium when requested

### Technical Details
- Added `GetCesiumGeoreference()` method for automatic georeference discovery
- Added `ConvertGPSToWorldPosition()` method with Cesium/Mercator switching
- Added `SampleCesiumTerrainHeight()` for ground clamping with line traces
- Modified `UpdateUserVisualization()` to use new conversion system
- TActorIterator search for ACesiumGeoreference in level
- glm::dvec3 coordinate transformation for WGS84 to Unreal conversion
- Collision-based terrain height sampling with complex trace support

### Use Cases
- **Global Tracking**: Track users across continents with accurate WGS84 positioning
- **Terrain Visualization**: Display GPS positions on real-world 3D terrain from Cesium
- **Flight Tracking**: Clamp aircraft to terrain or apply altitude offsets
- **Outdoor Sports**: Visualize hiking, cycling, or skiing routes on actual terrain
- **Vehicle Racing**: Show cars on accurate terrain with elevation data

### Comparison: Mercator vs Cesium
| Feature | Mercator Projection | Cesium Georeference |
|---------|-------------------|---------------------|
| **Accuracy** | Approximate (small areas) | Precise (WGS84 global) |
| **Terrain** | Manual height only | Real terrain height |
| **Scale** | Local/regional | Global |
| **Dependencies** | None | CesiumForUnreal plugin |
| **Performance** | Fast | Slightly slower |
| **Best For** | Simple projects, small areas | Large areas, terrain integration |

## [1.2.0] - 2025-12-11

### Added
- **Interpolation Buffer System** - Alternative smoothing method with guaranteed accuracy
  - Buffers recent GPS positions with timestamps in a circular buffer
  - Interpolates between past positions for smooth, accurate movement
  - Configurable buffer time delay (default: 200ms)
  - Adjustable buffer size for irregular update rates
- Interpolation buffer configuration properties in `AGPSTrackerVisualizerActor`:
  - `bUseInterpolationBuffer` - Enable interpolation buffer mode (default: false)
  - `InterpolationBufferTime` - Time delay for rendering in seconds (default: 0.2)
  - `MaxBufferSize` - Maximum positions to buffer (default: 10)
- Mode selection between dead reckoning and interpolation buffer
- Circular buffer implementation for efficient position storage
- Linear interpolation between buffered positions and rotations

### Improved
- Smoothing system now supports two distinct modes with different trade-offs
- Better handling of irregular GPS update rates with interpolation buffer
- More options for tuning visualization quality vs latency

### Technical Details
- Added `FBufferedPosition` struct for storing position, rotation, and timestamp
- Extended `FUserMarker` with position buffer and write index tracking
- `UpdateInterpolationBuffer()` method handles buffer-based rendering
- `CalculateInterpolatedPosition()` performs linear interpolation between buffer entries
- Automatic mode switching in `Tick()` based on user configuration
- Buffer supports both forward and backward position searches for interpolation

### Comparison
- **Dead Reckoning**: Real-time, predictive, can overshoot, best for live tracking
- **Interpolation Buffer**: 200ms delay, always accurate, no overshoot, best for replay/analysis

## [1.1.0] - 2025-12-11

### Added
- **Dead Reckoning System** - Smooth position prediction between GPS updates
  - Position interpolation using configurable smoothing factor
  - Velocity-based position extrapolation from speed and bearing
  - Damping system to prevent overshooting predicted positions
  - Configurable minimum speed threshold for prediction
  - Maximum extrapolation time limit to prevent runaway predictions
- Dead reckoning configuration properties in `AGPSTrackerVisualizerActor`:
  - `bEnableDeadReckoning` - Enable/disable dead reckoning (default: true)
  - `PositionSmoothingFactor` - Controls interpolation smoothness (default: 0.15)
  - `MaxExtrapolationTime` - Max time to predict beyond last update (default: 5.0s)
  - `MinSpeedForPrediction` - Min speed to apply prediction (default: 1.0 km/h)
  - `PredictionDampingFactor` - Reduces prediction over time (default: 0.8)
- Velocity vector calculation from GPS speed and bearing
- Smooth trail visualization using predicted positions

### Improved
- Marker movement now smooth and continuous instead of jumping between GPS updates
- Visual rotation now follows velocity direction when moving
- Trail points use predicted positions for smoother path visualization

### Technical Details
- Added `FUserMarker` dead reckoning state tracking:
  - `CurrentPosition` - Interpolated/predicted position
  - `TargetPosition` - Last GPS position
  - `VelocityVector` - World space velocity from speed/bearing
  - `LastUpdateTime` - Time of last GPS update
- `UpdateDeadReckoning()` method applies prediction every frame
- `CalculatePredictedPosition()` computes extrapolated position
- Automatic velocity vector calculation from GPS bearing and speed

## [1.0.0] - 2025-12-11

### Added
- Initial release of GPS Tracker plugin for Unreal Engine 5.3
- WebSocket client subsystem for real-time GPS tracking
- Automatic connection management with reconnection support
- Group-based user tracking and visualization
- `UGPSTrackerSubsystem` - Main subsystem for server communication
- `AGPSTrackerVisualizerActor` - Actor for 3D visualization of user positions
- `UGPSTrackerBlueprintLibrary` - Helper functions for GPS calculations
- Support for receiving position updates from server
- Support for sending position updates to server
- User marker visualization with customizable colors and sizes
- User name and speed labels above markers
- Movement trail visualization
- Group horn alert functionality
- Blueprint-friendly API with events and delegates
- GPS coordinate conversion utilities (Mercator projection)
- Haversine distance calculation between GPS coordinates
- Bearing calculation between GPS points
- Speed-based color gradient system
- Keep-alive ping/pong system for connection stability
- Comprehensive documentation (README.md, QUICKSTART.md)
- Example Blueprint setups and use cases

### Supported Features
- WebSocket Secure (WSS) connection to GPS tracker server
- Real-time position broadcasting and receiving
- Group-based user isolation
- Automatic user marker spawning and updating
- Customizable visualization parameters
- Blueprint and C++ support
- Event-driven architecture
- JSON message parsing
- Connection state management
- Error handling and reporting

### Server Compatibility
- Compatible with GPS Tracker Server v1.x
- Default server: `wss://gps-tracker-server-production-5900.up.railway.app`
- WebSocket protocol with JSON messages
- Message types: register, speed, users, ping, pong, group-horn

### Known Limitations
- Simple Mercator projection (not suitable for large geographic areas)
- No authentication beyond group name
- Markers use basic sphere mesh (customization via Blueprint)
- Trail visualization uses debug lines (performance impact with many users)

## Future Roadmap

### Planned for 1.3.0
- Material-based trail rendering for better performance
- Built-in UI widgets for connection management
- Persistent marker customization per user
- Support for custom marker meshes
- Automatic camera framing to show all users
- Distance and bearing display between users
- Session recording and playback
- Better coordinate projection options

### Planned for 1.4.0
- Authentication token support
- Encrypted message payload option
- Voice chat integration
- Private messaging between users
- User status indicators (online, offline, idle)
- Historical position data visualization
- Heatmap generation

### Planned for 2.0.0
- Unreal Engine 5.4+ support
- Enhanced Character Mobility System integration
- VR/AR support for marker visualization
- REST API integration for historical data
- Database connectivity for position history
- Admin dashboard integration
- Geofencing support
- Notification system

---

## Version History

| Version | Release Date | UE Version | Status |
|---------|--------------|------------|--------|
| 1.3.0   | 2025-12-11  | 5.3        | Current |
| 1.2.0   | 2025-12-11  | 5.3        | Previous |
| 1.1.0   | 2025-12-11  | 5.3        | Previous |
| 1.0.0   | 2025-12-11  | 5.3        | Initial |

---

For upgrade instructions and breaking changes, see the documentation.
