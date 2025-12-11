# Changelog

All notable changes to the GPS Tracker Unreal Plugin will be documented in this file.

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

### Planned for 1.2.0
- Material-based trail rendering for better performance
- Built-in UI widgets for connection management
- Persistent marker customization per user
- Support for custom marker meshes
- Automatic camera framing to show all users
- Distance and bearing display between users
- Session recording and playback
- Better coordinate projection options

### Planned for 1.3.0
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
| 1.1.0   | 2025-12-11  | 5.3        | Current |
| 1.0.0   | 2025-12-11  | 5.3        | Previous |

---

For upgrade instructions and breaking changes, see the documentation.
