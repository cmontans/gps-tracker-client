using Toybox.WatchUi;
using Toybox.System;

// Input delegate for handling button presses and gestures
class GPSTrackerDelegate extends WatchUi.BehaviorDelegate {

    private var _gpsManager;
    private var _statsManager;
    private var _communicationManager;

    function initialize(gpsManager, statsManager, communicationManager) {
        BehaviorDelegate.initialize();
        _gpsManager = gpsManager;
        _statsManager = statsManager;
        _communicationManager = communicationManager;
    }

    // Handle menu button press
    function onMenu() {
        var menu = new WatchUi.Menu2({:title => "Menu"});

        if (_gpsManager.isTracking()) {
            menu.addItem(new WatchUi.MenuItem(
                WatchUi.loadResource(Rez.Strings.Stop),
                null,
                :stop,
                {}
            ));
        } else {
            menu.addItem(new WatchUi.MenuItem(
                WatchUi.loadResource(Rez.Strings.Start),
                null,
                :start,
                {}
            ));
        }

        menu.addItem(new WatchUi.MenuItem(
            WatchUi.loadResource(Rez.Strings.Reset),
            null,
            :reset,
            {}
        ));

        menu.addItem(new WatchUi.MenuItem(
            WatchUi.loadResource(Rez.Strings.SendHorn),
            null,
            :horn,
            {}
        ));

        menu.addItem(new WatchUi.MenuItem(
            WatchUi.loadResource(Rez.Strings.Settings),
            null,
            :settings,
            {}
        ));

        WatchUi.pushView(menu, new $.GPSTrackerMenuDelegate(_gpsManager, _statsManager, _communicationManager), WatchUi.SLIDE_UP);
        return true;
    }

    // Handle select button (start/stop)
    function onSelect() {
        if (_gpsManager.isTracking()) {
            stopTracking();
        } else {
            startTracking();
        }
        WatchUi.requestUpdate();
        return true;
    }

    // Start tracking
    private function startTracking() {
        _gpsManager.startTracking();
        _communicationManager.sendTrackingState(true);
        System.println("Tracking started");
    }

    // Stop tracking
    private function stopTracking() {
        _gpsManager.stopTracking();
        _communicationManager.sendTrackingState(false);
        System.println("Tracking stopped");
    }

    // Handle back button
    function onBack() {
        if (_gpsManager.isTracking()) {
            // Ask for confirmation before exiting while tracking
            var dialog = new WatchUi.Confirmation("Stop tracking and exit?");
            WatchUi.pushView(dialog, new $.ConfirmationDelegate(method(:onExitConfirm)), WatchUi.SLIDE_IMMEDIATE);
            return true;
        }
        return false;
    }

    // Handle exit confirmation
    function onExitConfirm(confirmed) {
        if (confirmed) {
            _gpsManager.stopTracking();
            System.exit();
        }
    }
}

// Menu delegate for handling menu selections
class GPSTrackerMenuDelegate extends WatchUi.Menu2InputDelegate {

    private var _gpsManager;
    private var _statsManager;
    private var _communicationManager;

    function initialize(gpsManager, statsManager, communicationManager) {
        Menu2InputDelegate.initialize();
        _gpsManager = gpsManager;
        _statsManager = statsManager;
        _communicationManager = communicationManager;
    }

    function onSelect(item) {
        var id = item.getId();

        if (id == :start) {
            _gpsManager.startTracking();
            _communicationManager.sendTrackingState(true);
        } else if (id == :stop) {
            _gpsManager.stopTracking();
            _communicationManager.sendTrackingState(false);
        } else if (id == :reset) {
            _statsManager.reset();
            _communicationManager.sendResetStats();
        } else if (id == :horn) {
            _communicationManager.sendGroupHorn();
            System.println("Group horn sent");
        } else if (id == :settings) {
            // Open settings (would need separate settings view)
            System.println("Settings selected");
        }

        WatchUi.requestUpdate();
    }
}

// Confirmation delegate
class ConfirmationDelegate extends WatchUi.ConfirmationDelegate {

    private var _callback;

    function initialize(callback) {
        ConfirmationDelegate.initialize();
        _callback = callback;
    }

    function onResponse(response) {
        if (_callback != null) {
            _callback.invoke(response == WatchUi.CONFIRM_YES);
        }
    }
}
