using Toybox.System;
using Toybox.Lang;

// Statistics manager for tracking speed and location data
class StatsManager {

    private var _currentSpeed;
    private var _maxSpeed;
    private var _avgSpeed;
    private var _speedReadings;
    private var _latitude;
    private var _longitude;
    private var _bearing;

    const MAX_READINGS = 20;

    function initialize() {
        _currentSpeed = 0.0;
        _maxSpeed = 0.0;
        _avgSpeed = 0.0;
        _speedReadings = [];
        _latitude = 0.0;
        _longitude = 0.0;
        _bearing = 0.0;
    }

    // Update speed statistics
    function updateSpeed(speed) {
        _currentSpeed = speed;

        // Update max speed
        if (speed > _maxSpeed) {
            _maxSpeed = speed;
        }

        // Add to readings array
        _speedReadings.add(speed);

        // Keep only last MAX_READINGS
        if (_speedReadings.size() > MAX_READINGS) {
            _speedReadings = _speedReadings.slice(1, _speedReadings.size());
        }

        // Calculate average
        _avgSpeed = calculateAverage();
    }

    // Calculate average speed
    private function calculateAverage() {
        if (_speedReadings.size() == 0) {
            return 0.0;
        }

        var sum = 0.0;
        for (var i = 0; i < _speedReadings.size(); i++) {
            sum += _speedReadings[i];
        }

        return sum / _speedReadings.size();
    }

    // Update position
    function updatePosition(latitude, longitude, bearing) {
        _latitude = latitude;
        _longitude = longitude;
        _bearing = bearing;
    }

    // Reset statistics
    function reset() {
        _maxSpeed = 0.0;
        _avgSpeed = 0.0;
        _speedReadings = [];
        System.println("Stats reset");
    }

    // Getters
    function getCurrentSpeed() {
        return _currentSpeed;
    }

    function getMaxSpeed() {
        return _maxSpeed;
    }

    function getAvgSpeed() {
        return _avgSpeed;
    }

    function getLatitude() {
        return _latitude;
    }

    function getLongitude() {
        return _longitude;
    }

    function getBearing() {
        return _bearing;
    }

    // Format speed for display
    function formatSpeed(speed) {
        return speed.format("%.1f");
    }
}
