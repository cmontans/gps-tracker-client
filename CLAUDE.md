# CLAUDE.md - GPS Tracker Client

## Project Overview

**GPS Speed Tracker** is a real-time GPS tracking web application designed for mobile devices. It allows multiple users to track and share their speed data in real-time within private groups. The application is built as a Progressive Web App (PWA) that can be installed on mobile devices.

**Key Use Cases:**
- Real-time speed tracking for groups (racing, cycling, running)
- Viewer mode for spectators to follow participants
- Group-based isolation for privacy
- Live map visualization with user trajectories

**Current Version:** v4.0 - Viewer mode and fullscreen map support

## Repository Structure

```
gps-tracker-client/
├── index.html          # Single-file application (HTML + CSS + JavaScript)
├── .git/              # Git repository
└── CLAUDE.md          # This file
```

### Architecture: Single-File Application

This is intentionally a **single-file web application**. All HTML, CSS, and JavaScript are contained in `index.html`. This design choice enables:
- Easy deployment (single file upload)
- No build process required
- Immediate use without installation
- Simple hosting (any static file server)

**DO NOT** create separate files unless explicitly requested. Keep the single-file architecture intact.

## Technologies & Dependencies

### Core Technologies
- **Vanilla JavaScript** - No framework, pure ES6+
- **HTML5** - Semantic markup, PWA manifest
- **CSS** - Inline styles + Tailwind CSS

### External Libraries (CDN)
- **Tailwind CSS** (`https://cdn.tailwindcss.com`) - Utility-first CSS framework
- **Leaflet.js** (`https://unpkg.com/leaflet@1.9.4/`) - Interactive maps with OpenStreetMap

### Browser APIs
- **Geolocation API** - GPS tracking with high accuracy mode
- **WebSocket API** - Real-time bidirectional communication
- **Wake Lock API** - Prevent screen from sleeping during tracking
- **Web App Manifest** - PWA installation (embedded as base64)

### Server Communication
- **WebSocket Server**: `wss://gps-tracker-server-production-5900.up.railway.app`
- **Protocol**: JSON-based messages (`register`, `speed`, `users`, `ping/pong`)
- **Group System**: Case-sensitive group names for user isolation

## Development Workflow

### Git Practices

**Branch Naming Convention:**
- Feature branches: `claude/claude-md-{session-id}-{unique-id}`
- Example: `claude/claude-md-miu1mrnvcstlqa8i-01G3PPFnbupQXCRowQWJWEwm`

**Commit Messages:**
All commits follow a simple pattern:
- "Update index.html" for incremental changes
- "Create index.html" for initial file creation
- "Refactor {feature} for {reason}" for significant changes

**Git Push Requirements:**
- Always push to the designated claude branch
- Use: `git push -u origin <branch-name>`
- Retry up to 4 times with exponential backoff (2s, 4s, 8s, 16s) on network failures

### Making Changes

1. **Always read the file first** - Never modify code you haven't read
2. **Test in browser** - Changes should be validated by opening index.html
3. **Preserve structure** - Maintain the single-file architecture
4. **Commit incrementally** - Small, focused commits are preferred
5. **Keep version number updated** - Update the version string when adding features

## Code Structure & Conventions

### HTML Structure (Lines 1-349)

```
<head>
  - Meta tags for PWA, mobile optimization
  - Embedded manifest (base64)
  - Tailwind CSS CDN
  - Leaflet CSS
  - Inline styles

<body>
  - Header with settings button
  - Settings modal
  - Configuration section (name, group, viewer mode)
  - Status section (speed display, stats)
  - Users list
  - Map container
  - Fullscreen map overlay
  - Instructions sections
```

### JavaScript Architecture (Lines 350-1129)

**Global State Variables:**
- `userId` - Persistent user identifier (localStorage)
- `userName` - Display name (localStorage)
- `groupName` - Current group (localStorage)
- `serverUrl` - WebSocket endpoint (localStorage)
- `viewerMode` - Boolean, viewer-only mode
- `ws` - WebSocket connection
- `watchId` - Geolocation watch ID
- `isTracking` - Tracking state
- `users` - Array of connected users
- `wakeLock` - Wake Lock API reference
- `map`, `mapFullscreen` - Leaflet map instances
- `userMarkers`, `userTracks`, `trackPolylines` - Map visualization data

**Key Functions:**

1. **GPS & Tracking** (Lines 970-1107)
   - `startTracking()` - Initialize GPS watch and WebSocket
   - `stopTracking()` - Cleanup all resources
   - Geolocation watchPosition with high accuracy
   - Speed calculation: m/s → km/h conversion
   - Direction/bearing calculation between points

2. **WebSocket Communication** (Lines 851-921)
   - `connectWebSocket()` - Establish connection with auto-reconnect
   - Message types: `register`, `speed`, `users`, `ping/pong`, `join`
   - Exponential backoff: 1s → 30s max delay
   - Keep-alive ping every 25 seconds
   - Viewer mode: only receives, doesn't transmit position

3. **Map Visualization** (Lines 480-750)
   - `initMap(lat, lon, container)` - Initialize Leaflet instance
   - `updateMap()` - Update markers and trajectories
   - `createArrowIcon(speed, isCurrentUser, bearing)` - Custom direction markers
   - `calculateBearing(lat1, lon1, lat2, lon2)` - Compass direction
   - Track retention: last 50 points per user
   - Only draw tracks when speed > 3.6 km/h (1 m/s)

4. **UI Updates** (Lines 820-968)
   - `showError(message)` - Display error with auto-hide
   - `showSuccess(message)` - Display success message
   - `updateConnectionStatus(connected)` - WiFi icon state
   - `updateGpsStatus(active)` - GPS icon state
   - `updateUsersList()` - Render users with stats

5. **Wake Lock & Keep-Alive** (Lines 753-817)
   - `requestWakeLock()` - Prevent screen sleep (Android)
   - `releaseWakeLock()` - Release lock on stop
   - `startKeepAlive()` - 25s ping interval
   - Auto-reacquire on visibility change

### Important Code Patterns

**localStorage Persistence:**
```javascript
userId = localStorage.getItem('userId') || `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
localStorage.setItem('userId', userId);
```

**Speed Calculation:**
```javascript
const speed = position.coords.speed !== null
  ? Math.max(0, Math.round(position.coords.speed * 3.6))  // m/s to km/h
  : 0;
```

**Bearing Calculation:**
```javascript
function calculateBearing(lat1, lon1, lat2, lon2) {
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const y = Math.sin(dLon) * Math.cos(lat2 * Math.PI / 180);
  const x = Math.cos(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180) -
            Math.sin(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(dLon);
  const bearing = Math.atan2(y, x) * 180 / Math.PI;
  return (bearing + 360) % 360;
}
```

**WebSocket Message Format:**
```javascript
// Register (tracker mode)
{ type: 'register', userId, userName, groupName }

// Speed update
{ type: 'speed', userId, userName, groupName, speed, maxSpeed, lat, lon, bearing, timestamp }

// Join (viewer mode)
{ type: 'join', groupName, viewerMode: true }

// Users list (received)
{ type: 'users', users: [...] }
```

## Key Features & Implementation

### 1. Group-Based Isolation
- Users specify a `groupName` (case-sensitive)
- Server filters users by exact group match
- Enables private tracking sessions
- No cross-group visibility

### 2. Viewer Mode
- Checkbox: "Modo Visualizador"
- `viewerMode = true` → Only receive updates, don't transmit position
- Hides GPS status container (line 1105)
- Sends `join` message instead of `register` (lines 875-881)
- Useful for spectators, race observers

### 3. Real-Time Map
- **Normal View**: Toggle with "Ver Mapa" button
- **Fullscreen View**: Dedicated overlay with "Pantalla Completa"
- **Arrow Markers**: Custom SVG showing direction and speed
- **Track Polylines**: Last 50 points, only when speed > 3.6 km/h
- **Auto-center**: Follows current user unless in viewer mode

### 4. Statistics Tracking
- **Current Speed**: Real-time from GPS
- **Max Speed**: Session maximum (persistent across stops)
- **Average Speed**: Rolling average of last 20 readings
- **Reset**: Manual reset button (doesn't auto-reset on stop)

### 5. Wake Lock (Android)
- Prevents screen from sleeping during tracking
- iOS users must configure manually: Settings → Display → Auto-Lock → Never
- Auto-reacquires on page visibility change
- Visual indicator when active

### 6. Progressive Web App
- Inline manifest (base64 encoded)
- Standalone display mode
- Safe area insets for notched devices
- Install prompt on iOS Safari
- Detects standalone mode

### 7. iOS Compatibility
- Detects iOS devices (line 354)
- Shows special instructions for Safari
- Handles geolocation permissions differently
- Recommends "Add to Home Screen" workflow

## Testing & Validation

### Browser Testing
1. **Open in Browser**: Simply open `index.html` in a modern browser
2. **Enable HTTPS**: Some APIs require secure context (local file:// usually works)
3. **Mobile Testing**: Use browser DevTools device emulation or real device
4. **WebSocket**: Ensure server is accessible from your network

### Required Permissions
- **Location**: High accuracy GPS access
- **Wake Lock**: Screen wake (Android Chrome 84+)

### Testing Checklist
- [ ] Configuration inputs (name, group) save to localStorage
- [ ] WebSocket connection establishes successfully
- [ ] GPS tracking starts and displays speed
- [ ] Users list updates with multiple connections
- [ ] Map displays markers and tracks correctly
- [ ] Viewer mode receives but doesn't transmit
- [ ] Wake Lock activates (Android)
- [ ] Fullscreen map works
- [ ] Statistics calculate correctly
- [ ] Reconnection works after network loss

## Common Modification Tasks

### Adding a New UI Feature
1. Add HTML structure in appropriate section (lines 46-347)
2. Add event listener in JavaScript (lines 430-478)
3. Implement handler function
4. Update global state if needed
5. Test on mobile viewport

### Modifying WebSocket Protocol
1. Update message handlers in `ws.onmessage` (lines 886-895)
2. Add new message type handler
3. Update send calls in GPS watch (line 1036) or registration (line 866)
4. Coordinate with server changes

### Changing Map Behavior
1. Locate map functions (lines 480-750)
2. Modify `updateMap()` for marker/track logic
3. Adjust `createArrowIcon()` for marker appearance
4. Update Leaflet options in `initMap()`

### Adding Statistics
1. Add display element in HTML (around lines 174-193)
2. Initialize variable in global state (lines 362-389)
3. Update calculation in GPS watch callback (lines 997-1050)
4. Add to reset function (lines 471-478)

### Styling Changes
1. **Tailwind classes**: Modify class strings in HTML
2. **Inline styles**: Update `<style>` block (lines 27-43)
3. **Tailwind config**: Modify config object (lines 17-25)
4. **Map styling**: CSS in Leaflet icon creation (lines 513-546)

## Important Constraints & Considerations

### DO NOT:
- Split into multiple files unless explicitly requested
- Remove the single-file architecture
- Add build tools or bundlers
- Break backward compatibility with existing localStorage data
- Modify WebSocket protocol without server coordination
- Remove iOS compatibility code
- Disable high accuracy GPS mode

### DO:
- Preserve localStorage keys (`userId`, `userName`, `groupName`, `serverUrl`)
- Maintain WebSocket reconnection logic
- Keep wake lock functionality
- Preserve group isolation feature
- Test on both iOS and Android when possible
- Update version number for feature additions (line 54)
- Handle GPS permission errors gracefully
- Maintain responsive design (mobile-first)

### Performance Considerations
- Map track limit: 50 points (line 599) - prevents memory bloat
- Keep-alive interval: 25s (line 809) - balances latency and overhead
- Speed readings for average: 20 samples (line 1015)
- Error auto-hide: 8s (line 825), success: 3s (line 834)
- Max reconnect attempts: 10 (line 376)
- Reconnect max delay: 30s (line 910)

### Security Notes
- WebSocket uses WSS (secure) connection
- No authentication implemented - group name is the "password"
- User IDs are client-generated (not secure, but sufficient for use case)
- No sensitive data stored
- All communication is unencrypted beyond WSS

## Mobile-Specific Features

### Android
- Wake Lock API prevents screen sleep automatically
- Overscroll behavior contained (line 29)
- Tap highlight disabled (line 30)
- Chrome 84+ recommended

### iOS
- Manual screen lock configuration required
- Safari-specific geolocation permissions
- PWA installation via Share menu
- Safe area insets for notched devices (lines 37-42)
- Standalone mode detection (line 355)

### PWA Capabilities
- Can be installed to home screen
- Standalone display mode
- Custom icon (SVG embedded as base64)
- Offline manifest included
- No service worker (app requires network)

## Debugging

### Console Logging
The app includes extensive console logging:
- `🚀` - Initialization
- `✓` - Success operations
- `❌` - Errors
- `⚠️` - Warnings
- `🔌` - WebSocket events
- `📍` - GPS updates
- `💓` - Keep-alive pings
- `↻` - Reconnection attempts

### Common Issues

**GPS not working:**
- Check browser permissions
- Verify HTTPS/secure context
- Ensure device GPS is enabled
- Try outdoor location for better signal

**WebSocket connection fails:**
- Verify server URL is correct
- Check server is running
- Test with browser console Network tab
- Verify WSS (not WS) for secure connection

**Wake Lock not activating:**
- Android Chrome 84+ required
- iOS not supported (manual configuration needed)
- Check browser console for errors

**Users not appearing:**
- Verify exact group name match (case-sensitive)
- Check WebSocket connection status
- Confirm server is broadcasting users
- Check browser console for message logs

## Version History

- **v4.0**: Viewer mode and fullscreen map support
- **v2.1**: Wake Lock API, group system, map improvements
- **Earlier**: Basic GPS tracking, WebSocket communication

## Future Enhancement Ideas

When implementing new features, consider:
- Offline mode with service worker
- Historical track replay
- Export tracks to GPX/KML
- Customizable speed units (mph, knots)
- Audio alerts for speed milestones
- Battery optimization modes
- Server authentication
- End-to-end encryption for groups
- Track sharing/social features

## Related Repositories

This client connects to a WebSocket server (not included in this repository):
- Server URL: `wss://gps-tracker-server-production-5900.up.railway.app`
- Assumed to handle: User registration, message broadcasting, group filtering

---

**Last Updated**: 2025-12-06
**Maintainer**: cmontans
**AI Assistant**: Use this document as your primary reference for understanding and modifying this codebase.
