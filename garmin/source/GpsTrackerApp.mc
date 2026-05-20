import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;
import Toybox.Position;
import Toybox.Communications;
import Toybox.Application.Storage;

class GpsTrackerApp extends Application.AppBase {

    private var jumpDetector;
    public var currentSpeed = 0.0;
    public var currentAltitude = 0.0;
    public var maxJumpHeight = 0.0;

    function initialize() {
        AppBase.initialize();
        jumpDetector = new JumpDetector(self);
    }

    // onStart() is called on application start up
    function onStart(state as Dictionary?) as Void {
        Position.enableLocationEvents(Position.LOCATION_CONTINUOUS, method(:onPosition));
        if (jumpDetector != null) {
            jumpDetector.start();
        }
    }

    // onStop() is called when your application is exiting
    function onStop(state as Dictionary?) as Void {
        Position.enableLocationEvents(Position.LOCATION_DISABLE, method(:onPosition));
        if (jumpDetector != null) {
            jumpDetector.stop();
        }
    }

    // Return the initial view of your application here
    function getInitialView() {
        return [ new GpsTrackerView(), new GpsTrackerDelegate() ];
    }
    
    function onPosition(info as Position.Info) as Void {
        // Broadcast location updates to the view or model
        currentSpeed = info.speed;
        currentAltitude = info.altitude;
        WatchUi.requestUpdate();

        var message = {
            "type" => "location",
            "speed" => info.speed,
            "altitude" => info.altitude
        };
        try {
            Communications.transmit(message, null, new CommListener());
        } catch(e) {
            // Companion app might not be connected
        }
    }

    function onJumpDetected(height, hangtime) {
        if (height > maxJumpHeight) {
            maxJumpHeight = height;
        }
        
        // Save to History using Storage
        var jumpHistory = Storage.getValue("jumpHistory") as Toybox.Lang.Array<Toybox.Lang.Dictionary>;
        if (jumpHistory == null) {
            jumpHistory = [] as Toybox.Lang.Array<Toybox.Lang.Dictionary>;
        }
        
        var newJump = {
            "height" => height,
            "hangtime" => hangtime,
            "time" => System.getTimer()
        } as Toybox.Lang.Dictionary;
        
        jumpHistory.add(newJump);
        // Keep max 50 jumps
        if (jumpHistory.size() > 50) {
            jumpHistory = jumpHistory.slice(1, jumpHistory.size());
        }
        Storage.setValue("jumpHistory", jumpHistory);
        
        var message = {
            "type" => "jump",
            "height" => height,
            "hangtime" => hangtime
        };
        try {
            Communications.transmit(message, null, new CommListener());
        } catch(e) {
            // Companion app might not be connected
        }

        // Output jump to UI update
        WatchUi.requestUpdate();
    }

}
