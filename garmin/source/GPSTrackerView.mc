using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.System;
using Toybox.Lang;

// Main view for GPS Tracker
class GPSTrackerView extends WatchUi.View {

    private var _gpsManager;
    private var _statsManager;
    private var _communicationManager;

    // Display dimensions
    private var _width;
    private var _height;
    private var _centerX;
    private var _centerY;

    function initialize(gpsManager, statsManager, communicationManager) {
        View.initialize();
        _gpsManager = gpsManager;
        _statsManager = statsManager;
        _communicationManager = communicationManager;
    }

    // Load resources
    function onLayout(dc) {
        _width = dc.getWidth();
        _height = dc.getHeight();
        _centerX = _width / 2;
        _centerY = _height / 2;
    }

    // Update the view
    function onUpdate(dc) {
        // Clear the screen
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();

        // Draw current speed (large and centered)
        drawCurrentSpeed(dc);

        // Draw statistics (max and avg)
        drawStatistics(dc);

        // Draw status indicators
        drawStatusIndicators(dc);

        // Draw tracking indicator
        if (_gpsManager.isTracking()) {
            drawTrackingIndicator(dc);
        }
    }

    // Draw large current speed
    private function drawCurrentSpeed(dc) {
        var speed = _statsManager.getCurrentSpeed();
        var speedText = _statsManager.formatSpeed(speed);

        // Speed label
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX,
            _centerY - 60,
            Graphics.FONT_TINY,
            WatchUi.loadResource(Rez.Strings.CurrentSpeed),
            Graphics.TEXT_JUSTIFY_CENTER
        );

        // Large speed value
        dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX,
            _centerY - 35,
            Graphics.FONT_NUMBER_HOT,
            speedText,
            Graphics.TEXT_JUSTIFY_CENTER
        );

        // Unit
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX,
            _centerY + 25,
            Graphics.FONT_SMALL,
            WatchUi.loadResource(Rez.Strings.SpeedUnit),
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }

    // Draw max and average speed
    private function drawStatistics(dc) {
        var maxSpeed = _statsManager.getMaxSpeed();
        var avgSpeed = _statsManager.getAvgSpeed();

        var y = _centerY + 55;

        // Max speed
        dc.setColor(Graphics.COLOR_ORANGE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX - 40,
            y,
            Graphics.FONT_XTINY,
            WatchUi.loadResource(Rez.Strings.MaxSpeed),
            Graphics.TEXT_JUSTIFY_CENTER
        );
        dc.drawText(
            _centerX - 40,
            y + 15,
            Graphics.FONT_SMALL,
            _statsManager.formatSpeed(maxSpeed),
            Graphics.TEXT_JUSTIFY_CENTER
        );

        // Average speed
        dc.setColor(Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX + 40,
            y,
            Graphics.FONT_XTINY,
            WatchUi.loadResource(Rez.Strings.AvgSpeed),
            Graphics.TEXT_JUSTIFY_CENTER
        );
        dc.drawText(
            _centerX + 40,
            y + 15,
            Graphics.FONT_SMALL,
            _statsManager.formatSpeed(avgSpeed),
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }

    // Draw GPS and connection status
    private function drawStatusIndicators(dc) {
        var y = 20;

        // GPS status
        var gpsColor = _gpsManager.hasGoodGPS() ?
            Graphics.COLOR_GREEN : Graphics.COLOR_RED;
        dc.setColor(gpsColor, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX - 40,
            y,
            Graphics.FONT_XTINY,
            _gpsManager.getGPSQualityString(),
            Graphics.TEXT_JUSTIFY_CENTER
        );

        // Phone connection status
        var phoneColor = _communicationManager.isPhoneConnected() ?
            Graphics.COLOR_GREEN : Graphics.COLOR_RED;
        dc.setColor(phoneColor, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            _centerX + 40,
            y,
            Graphics.FONT_XTINY,
            _communicationManager.isPhoneConnected() ? "Phone" : "No Phone",
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }

    // Draw tracking indicator (small circle)
    private function drawTrackingIndicator(dc) {
        dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
        dc.fillCircle(_width - 15, 15, 5);
    }

    // Called when this View is brought to the foreground
    function onShow() {
    }

    // Called when this View is removed from the screen
    function onHide() {
    }
}
