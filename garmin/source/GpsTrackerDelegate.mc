import Toybox.Lang;
import Toybox.WatchUi;

class GpsTrackerDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onSelect() as Boolean {
        // Open the Jump History Menu
        JumpHistoryMenu.push();
        return true;
    }

}
