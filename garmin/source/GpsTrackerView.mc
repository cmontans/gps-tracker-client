import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Application;
import Toybox.Lang;

class GpsTrackerView extends WatchUi.View {

    private var _speedLabel;
    private var _jumpLabel;
    private var _statusLabel;

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
        setLayout(Rez.Layouts.MainLayout(dc));
        _speedLabel = View.findDrawableById("SpeedLabel");
        _jumpLabel = View.findDrawableById("JumpLabel");
        _statusLabel = View.findDrawableById("StatusLabel");
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() as Void {
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {
        var app = Application.getApp() as GpsTrackerApp;
        var speed = app.currentSpeed;
        
        if (speed != null) {
            // Speed comes in m/s, convert to km/h or knots
            var speedStr = (speed * 1.94384).format("%.1f") + " kts";
            _speedLabel.setText(speedStr);
            _statusLabel.setText("Tracking");
        }
        
        // Jump label logic placeholder
        var maxJump = app.maxJumpHeight;
        if (maxJump != null) {
            _jumpLabel.setText("Max Jump: " + maxJump.format("%.1f") + "m");
        } else {
            _jumpLabel.setText("Max Jump: 0.0m");
        }

        // Call the parent onUpdate function to redraw the layout
        View.onUpdate(dc);
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() as Void {
    }

}
