# Changelog

All notable changes to the GPS Tracker Unity Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-12-11

### Added

#### Core Features
- **GPSTrackerManager**: Singleton manager for WebSocket connection and message handling
- **GPSTrackerVisualizer**: Component for automatic 3D visualization of user positions
- **GPSUserData**: Data class for user position information
- **GPSUtilities**: Static utility class with GPS conversion and calculation functions

#### Connection Features
- WebSocket connection to GPS tracker server via NativeWebSocket library
- Automatic user ID generation
- Group-based isolation (only see users in same group)
- Automatic reconnection handling
- Keep-alive ping/pong system
- Connection state tracking and events

#### Visualization Features
- Automatic marker creation and updating for all group members
- Default sphere markers with customizable size and color
- Custom marker prefab support
- User name labels (TextMesh components)
- Speed display labels with color coding
- Movement trail visualization using LineRenderer
- Configurable trail length and appearance
- Billboard text components (always face camera)

#### Position Smoothing
- **Dead Reckoning System**:
  - Position prediction between GPS updates using velocity and bearing
  - Configurable smoothing factor for interpolation
  - Maximum extrapolation time limit
  - Minimum speed threshold for prediction
  - Damping factor to reduce overshooting
  - Real-time responsive movement

- **Interpolation Buffer System**:
  - Alternative smoothing using buffered past positions
  - Configurable buffer size and delay time
  - Guaranteed accuracy (no prediction overshooting)
  - Smooth interpolation between buffered positions
  - Better for replay/analysis scenarios

#### GPS Utilities
- GPS to Unity world position conversion (Mercator projection)
- Unity world position to GPS conversion
- Haversine distance calculation between coordinates
- Bearing calculation between coordinates
- Speed formatting for display
- Speed-based color generation (green to red gradient)
- Unique user ID generation
- Current timestamp utilities

#### Event System
- Connection state changed events
- User position update events
- Group horn alert events
- Error handling events
- Visualization update events

#### Message Protocol
- Register message (join group)
- Speed message (position update)
- Group horn message (alert all users)
- Ping/pong keep-alive messages
- Users broadcast (receive all group positions)

#### Documentation
- Comprehensive README.md with features and usage
- QUICKSTART.md for 5-minute setup guide
- API_REFERENCE.md with complete API documentation
- CHANGELOG.md for version tracking
- Inline code documentation and XML comments

#### Unity Integration
- Unity Package Manager support (package.json)
- Assembly definition file for proper module separation
- Compatible with Unity 2020.3 and higher
- Cross-platform support (Windows, macOS, Linux, Android, iOS)
- DontDestroyOnLoad singleton pattern for manager persistence

### Technical Details

#### Dependencies
- Unity 2020.3 or higher
- NativeWebSocket library (endel/NativeWebSocket)
- Newtonsoft.Json for JSON parsing

#### Architecture
- Singleton pattern for GPSTrackerManager
- Event-driven architecture for loose coupling
- Component-based visualization system
- Efficient marker pooling and reuse
- Performance-optimized trail rendering

#### Server Communication
- Default server: `wss://gps-tracker-server-production-5900.up.railway.app`
- JSON message format
- WebSocket Secure (WSS) protocol
- Automatic message queue dispatching
- Async/await pattern for connection handling

### Known Limitations

- WebGL platform requires alternative WebSocket implementation (NativeWebSocket limitation)
- TextMesh components used for labels (consider TextMeshPro for better quality)
- Default Mercator projection (simple, suitable for small areas)
- No built-in SSL certificate validation customization

### Performance Characteristics

- Negligible CPU overhead for connection management
- LineRenderer used for trails (GPU-accelerated)
- Event-based updates (no polling)
- Efficient marker GameObject reuse
- Configurable trail points for performance tuning

---

## [Unreleased]

### Planned Features

#### v1.1.0 - Enhanced Visualization
- TextMeshPro support for labels
- Custom shader support for markers
- Particle effects for movement
- Speed-based marker scaling
- Distance-based LOD system
- Minimap visualization support

#### v1.2.0 - Advanced Features
- Replay system for recorded sessions
- Historical trail playback
- User filtering and grouping
- Custom message types
- Encryption support for messages
- Authentication system integration

#### v1.3.0 - Mobile Optimization
- Touch gesture controls
- Mobile-optimized shaders
- Battery usage optimization
- Network data compression
- Offline mode with queuing

#### v2.0.0 - Major Enhancements
- Multiple server support
- Load balancing
- Server discovery
- Custom protocols
- Advanced interpolation algorithms
- Machine learning-based prediction
- AR/VR support optimizations

### Under Consideration

- Integration with Unity's new Input System
- Support for other projection systems (UTM, etc.)
- Built-in geocoding services
- Map tile integration
- Weather data overlay
- Traffic data integration
- Route planning features

---

## Version Migration Guides

### Migrating to v1.0.0

This is the initial release. No migration needed.

---

## Breaking Changes

### v1.0.0
- Initial release - no breaking changes

---

## Security Notes

### v1.0.0
- Uses WebSocket Secure (WSS) by default
- User IDs can be auto-generated or provided
- No built-in authentication (relies on server-side implementation)
- Group isolation prevents cross-group data access
- Messages are not encrypted beyond WSS transport encryption

**Recommendations:**
- Use the default WSS server for encrypted transport
- Implement server-side authentication if needed
- Validate all user input before sending to server
- Use unique group names for privacy
- Don't include sensitive data in user names or messages

---

## Support and Compatibility

### Unity Version Compatibility

| Unity Version | Status | Notes |
|--------------|--------|-------|
| 2020.3 LTS | ✅ Supported | Recommended minimum version |
| 2021.3 LTS | ✅ Supported | Fully tested |
| 2022.3 LTS | ✅ Supported | Fully tested |
| 2023.x | ✅ Supported | Compatible |
| 6000.x | ✅ Supported | Compatible |

### Platform Compatibility

| Platform | Status | Notes |
|----------|--------|-------|
| Windows | ✅ Supported | Fully tested |
| macOS | ✅ Supported | Fully tested |
| Linux | ✅ Supported | Compatible |
| Android | ✅ Supported | Compatible |
| iOS | ✅ Supported | Compatible |
| WebGL | ⚠️ Limited | Requires alternative WebSocket library |
| Console | 🔄 Untested | Should work with platform-specific WebSocket |

### Dependency Versions

| Dependency | Minimum Version | Recommended Version |
|------------|----------------|-------------------|
| Unity | 2020.3 | 2021.3 LTS or newer |
| NativeWebSocket | 1.1.0 | Latest |
| Newtonsoft.Json | 3.0.2 | Latest |

---

## Contributors

- GPS Tracker Team

---

## License

MIT License - See LICENSE file for details.

---

For more information:
- [README.md](README.md) - Overview and features
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [API_REFERENCE.md](API_REFERENCE.md) - Complete API documentation
