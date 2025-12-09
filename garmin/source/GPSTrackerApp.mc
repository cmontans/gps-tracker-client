using Toybox.Application;
using Toybox.WatchUi;
using Toybox.Position;
using Toybox.System;
using Toybox.Communications;

// Main application class
class GPSTrackerApp extends Application.AppBase {

    private var _mainView;
    private var _gpsManager;
    private var _statsManager;
    private var _communicationManager;

    function initialize() {
        AppBase.initialize();

        // Initialize managers
        _statsManager = new $.StatsManager();
        _gpsManager = new $.GPSManager(_statsManager);
        _communicationManager = new $.CommunicationManager();
    }

    // Return the initial view of your application
    function getInitialView() {
        _mainView = new $.GPSTrackerView(_gpsManager, _statsManager, _communicationManager);
        var delegate = new $.GPSTrackerDelegate(_gpsManager, _statsManager, _communicationManager);
        return [_mainView, delegate];
    }

    // Handle application start
    function onStart(state) {
        System.println("App started");
        Position.enableLocationEvents(Position.LOCATION_CONTINUOUS, method(:onPosition));
    }

    // Handle application stop
    function onStop(state) {
        System.println("App stopped");
        Position.enableLocationEvents(Position.LOCATION_DISABLE, method(:onPosition));
        _gpsManager.stopTracking();
    }

    // Handle position updates
    function onPosition(info) {
        if (_gpsManager != null) {
            _gpsManager.onPosition(info);
        }
    }

    // Get GPS manager instance
    function getGPSManager() {
        return _gpsManager;
    }

    // Get stats manager instance
    function getStatsManager() {
        return _statsManager;
    }

    // Get communication manager instance
    function getCommunicationManager() {
        return _communicationManager;
    }
}
