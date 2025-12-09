using Toybox.Position;
using Toybox.System;
using Toybox.Time;

// GPS and location tracking manager
class GPSManager {

    private var _isTracking;
    private var _gpsQuality;
    private var _lastPosition;
    private var _statsManager;

    const MS_TO_KMH = 3.6;

    function initialize(statsManager) {
        _isTracking = false;
        _gpsQuality = Position.QUALITY_NOT_AVAILABLE;
        _lastPosition = null;
        _statsManager = statsManager;
    }

    // Start tracking
    function startTracking() {
        _isTracking = true;
        System.println("Tracking started");
    }

    // Stop tracking
    function stopTracking() {
        _isTracking = false;
        System.println("Tracking stopped");
    }

    // Check if tracking is active
    function isTracking() {
        return _isTracking;
    }

    // Handle position update from GPS
    function onPosition(info) {
        if (info != null) {
            _gpsQuality = info.accuracy;
            _lastPosition = info.position;

            if (_isTracking) {
                processPosition(info);
            }
        }
    }

    // Process position data
    private function processPosition(info) {
        var speed = 0.0;

        // Get speed from GPS
        if (info has :speed && info.speed != null) {
            speed = info.speed * MS_TO_KMH; // Convert m/s to km/h
        }

        // Get bearing
        var bearing = 0.0;
        if (info has :heading && info.heading != null) {
            bearing = info.heading;
        }

        // Get coordinates
        var latitude = 0.0;
        var longitude = 0.0;
        if (info.position != null) {
            var coords = info.position.toDegrees();
            latitude = coords[0];
            longitude = coords[1];
        }

        // Update statistics
        _statsManager.updateSpeed(speed);
        _statsManager.updatePosition(latitude, longitude, bearing);
    }

    // Get GPS quality
    function getGPSQuality() {
        return _gpsQuality;
    }

    // Check if GPS has good signal
    function hasGoodGPS() {
        return _gpsQuality >= Position.QUALITY_USABLE;
    }

    // Get GPS quality string
    function getGPSQualityString() {
        switch (_gpsQuality) {
            case Position.QUALITY_NOT_AVAILABLE:
                return "No GPS";
            case Position.QUALITY_LAST_KNOWN:
                return "Last Known";
            case Position.QUALITY_POOR:
                return "Poor";
            case Position.QUALITY_USABLE:
                return "Usable";
            case Position.QUALITY_GOOD:
                return "Good";
            default:
                return "Unknown";
        }
    }

    // Get last known position
    function getLastPosition() {
        return _lastPosition;
    }
}
