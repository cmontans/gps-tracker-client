using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Application;
using Toybox.Position;
using Toybox.System;

// Data field showing current speed for watch faces
class SpeedDataField extends WatchUi.SimpleDataField {

    private var _speed;
    const MS_TO_KMH = 3.6;

    function initialize() {
        SimpleDataField.initialize();
        _speed = 0.0;

        // Set the label
        label = "Speed";

        // Enable position events
        Position.enableLocationEvents(Position.LOCATION_CONTINUOUS, method(:onPosition));
    }

    // Handle position updates
    function onPosition(info) {
        if (info has :speed && info.speed != null) {
            _speed = info.speed * MS_TO_KMH;
        }
    }

    // Compute the data value
    function compute(info) {
        if (info has :currentSpeed && info.currentSpeed != null) {
            _speed = info.currentSpeed * MS_TO_KMH;
        }

        return _speed.format("%.1f");
    }

    // Display the value
    function onUpdate(dc) {
        // Set colors
        dc.setColor(getBackgroundColor(), getBackgroundColor());
        dc.clear();

        // Display the value
        dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            dc.getWidth() / 2,
            dc.getHeight() / 2,
            Graphics.FONT_NUMBER_MEDIUM,
            _speed.format("%.1f"),
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );

        // Display the unit
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            dc.getWidth() / 2,
            dc.getHeight() / 2 + 25,
            Graphics.FONT_TINY,
            "km/h",
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }
}
