# KML Network Link - Real-time GPS Tracker Visualization

This feature allows you to view real-time positions of all active GPS tracker users in Google Earth, Google Maps, or any KML-compatible application.

## Overview

The GPS Tracker server now provides KML (Keyhole Markup Language) endpoints with NetworkLink functionality, which automatically refreshes user positions at regular intervals.

## Endpoints

### 1. Network Link Endpoint (Primary)
**URL:** `GET /kml/network-link`

This is the main endpoint you should open in Google Earth or other KML viewers. It creates an auto-refreshing network link that updates user positions automatically.

**Query Parameters:**
- `group` (optional) - Filter users by group name
  - Example: `/kml/network-link?group=MyTeam`
- `refresh` (optional) - Refresh interval in seconds (default: 5)
  - Example: `/kml/network-link?refresh=10`

**Examples:**
```
# All users, 5-second refresh
http://your-server:3001/kml/network-link

# Specific group, 5-second refresh
http://your-server:3001/kml/network-link?group=TeamA

# All users, 10-second refresh
http://your-server:3001/kml/network-link?refresh=10

# Specific group, custom refresh
http://your-server:3001/kml/network-link?group=TeamA&refresh=3
```

### 2. User Data Endpoint (Auto-refreshed by NetworkLink)
**URL:** `GET /kml/users`

This endpoint provides the actual KML data with user positions. It's automatically called by the NetworkLink, but you can also access it directly for static snapshots.

**Query Parameters:**
- `group` (optional) - Filter users by group name

## How to Use

### Google Earth Desktop

1. Open Google Earth
2. Go to **File → Open** or press `Ctrl+O`
3. Enter the network link URL in the location bar:
   ```
   http://YOUR_SERVER_IP:3001/kml/network-link
   ```
4. The network link will appear in your Places panel
5. User positions will automatically update every 5 seconds

### Google Earth Web

1. Open [Google Earth Web](https://earth.google.com/web/)
2. Click the **"Import KML file"** button (📂 icon on the left sidebar)
3. Enter the network link URL:
   ```
   http://YOUR_SERVER_IP:3001/kml/network-link
   ```
4. Note: Some browsers may require HTTPS for network links to work properly

### Google Maps (Manual Import)

1. Open [Google My Maps](https://www.google.com/maps/d/)
2. Create a new map or open an existing one
3. Click **"Import"** in the left panel
4. Paste the KML users endpoint URL:
   ```
   http://YOUR_SERVER_IP:3001/kml/users
   ```
5. Note: Google Maps doesn't support auto-refresh NetworkLinks, so you'll need to manually refresh

### Other KML Viewers

Any application that supports KML 2.2 NetworkLinks should work, including:
- QGIS
- ArcGIS
- NASA WorldWind
- Many mobile mapping apps

## Features

### Real-time Updates
- User positions automatically refresh at the specified interval (default: 5 seconds)
- No need to manually reload the file

### User Information Display
Each user marker shows:
- 🟢 User name with status indicator
  - 🟢 Green: Updated within 5 seconds (very fresh)
  - 🟡 Yellow: Updated 5-10 seconds ago (fresh)
  - 🟠 Orange: Updated 10-30 seconds ago (slightly stale)
- User ID and group name
- Current speed (km/h)
- Maximum speed recorded (km/h)
- Bearing/heading (degrees)
- Time since last update
- Exact timestamp

### Group Filtering
View only users from a specific group by adding the `?group=` parameter:
```
http://YOUR_SERVER_IP:3001/kml/network-link?group=MyTeam
```

### Active User Detection
- Users are automatically filtered if they haven't sent an update in 30+ seconds
- This ensures the map only shows truly active users

## Technical Details

### KML Structure

The implementation uses two KML files:

1. **NetworkLink Document** (`/kml/network-link`)
   - Contains a `<NetworkLink>` element
   - Configured with `refreshMode="onInterval"`
   - Points to the data endpoint

2. **Data Document** (`/kml/users`)
   - Contains `<Placemark>` elements for each user
   - Includes styled markers and information bubbles
   - Generated dynamically from active WebSocket connections

### Marker Styling
- Green circular markers with 1.2x scale
- Labels show user name with status emoji
- Click markers to see detailed information popup

### Performance
- Lightweight XML generation
- In-memory data source (no database queries)
- Efficient filtering and timestamp checking
- Minimal bandwidth usage with small refresh intervals

## Security Considerations

⚠️ **Important:** These endpoints are currently public and unauthenticated.

If you need to secure access:
1. Deploy behind a reverse proxy (nginx, Apache)
2. Add authentication middleware to the Express routes
3. Use HTTPS to encrypt data in transit
4. Implement rate limiting to prevent abuse

## Troubleshooting

### Network Link Not Updating
- Check that your KML viewer supports NetworkLink auto-refresh
- Verify the server is running and accessible
- Check browser/app console for CORS or security errors
- Try using a longer refresh interval (10+ seconds)

### No Users Showing
- Verify users are actively sending GPS data via the mobile app
- Check the `/health` endpoint to confirm active users
- Ensure you're using the correct group name if filtering
- Users older than 30 seconds are automatically filtered out

### CORS Errors (Browser-based viewers)
The server already has CORS enabled, but if you encounter issues:
- Ensure the server is accessible from your network
- Try using HTTPS instead of HTTP
- Check browser console for specific error messages

## Example Use Cases

1. **Team Tracking**: View all team members during a group ride or event
2. **Event Monitoring**: Monitor participants in real-time during races or competitions
3. **Fleet Management**: Track multiple vehicles or assets
4. **Emergency Response**: Coordinate teams with live position data
5. **Geographic Analysis**: Export and analyze movement patterns

## API Response Example

### Network Link Response
```xml
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>GPS Tracker - Live Network Link</name>
    <NetworkLink>
      <name>Active Users</name>
      <description>Updates every 5 seconds</description>
      <Link>
        <href>http://localhost:3001/kml/users</href>
        <refreshMode>onInterval</refreshMode>
        <refreshInterval>5</refreshInterval>
      </Link>
    </NetworkLink>
  </Document>
</kml>
```

### User Data Response
```xml
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>GPS Tracker - Active Users</name>
    <Style id="userStyle">
      <IconStyle>
        <color>ff00ff00</color>
        <scale>1.2</scale>
        <Icon>
          <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>
        </Icon>
      </IconStyle>
    </Style>

    <Placemark>
      <name>🟢 John Doe</name>
      <description><![CDATA[
        <b>User:</b> John Doe (user123)<br/>
        <b>Group:</b> TeamA<br/>
        <b>Speed:</b> 45.5 km/h<br/>
        <b>Max Speed:</b> 67.2 km/h<br/>
        <b>Bearing:</b> 180°<br/>
        <b>Last Update:</b> 2s ago<br/>
      ]]></description>
      <styleUrl>#userStyle</styleUrl>
      <Point>
        <coordinates>-122.4194,37.7749,0</coordinates>
      </Point>
    </Placemark>
  </Document>
</kml>
```

## Future Enhancements

Potential improvements for future versions:
- [ ] Historical track lines (show path traveled)
- [ ] Speed-based color coding (green/yellow/red markers)
- [ ] Bearing-based directional arrows
- [ ] Custom icons for different user types
- [ ] Time range filtering
- [ ] KMZ support (compressed KML with custom icons)
- [ ] Authentication and access control
- [ ] WebSocket-based push updates (instead of polling)

## Support

For issues or questions about the KML Network Link feature:
1. Check the server logs for error messages
2. Test the endpoints directly in a browser
3. Verify WebSocket connections are active (`/health` endpoint)
4. Review this documentation for common solutions

## References

- [KML 2.2 Specification](https://developers.google.com/kml/documentation/kmlreference)
- [Google Earth NetworkLink Documentation](https://developers.google.com/kml/documentation/kml_tut#networklinks)
- [GPS Tracker Server API Documentation](./README.md)
