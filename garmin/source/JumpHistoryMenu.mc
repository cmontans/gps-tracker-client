import Toybox.WatchUi;
import Toybox.Application.Storage;
import Toybox.System;

class JumpHistoryMenuDelegate extends WatchUi.Menu2InputDelegate {
    function initialize() {
        Menu2InputDelegate.initialize();
    }

    function onSelect(item) {
        // Just acknowledging selection, no further action needed for now
        System.println("Selected jump: " + item.getId());
    }
}

class JumpHistoryMenu {
    static function push() {
        var menu = new WatchUi.Menu2({:title=>"Jump History"});
        
        var history = Storage.getValue("jumpHistory");
        if (history != null && history.size() > 0) {
            // Add items in reverse order (newest first)
            for (var i = history.size() - 1; i >= 0; i--) {
                var jumpVal = history[i];
                if (jumpVal instanceof Toybox.Lang.Dictionary) {
                    var jump = jumpVal as Toybox.Lang.Dictionary;
                    var height = jump.get("height") as Toybox.Lang.Float;
                    var hangtime = jump.get("hangtime") as Toybox.Lang.Number;
                    
                    var heightStr = (height != null ? height : 0.0).format("%.1f") + "m";
                    var hangtimeStr = ((hangtime != null ? hangtime : 0) / 1000.0).format("%.1f") + "s";
                
                menu.addItem(
                    new WatchUi.MenuItem(
                        heightStr,
                        hangtimeStr,
                        "jump_" + i,
                        {}
                    )
                );
                }
            }
        } else {
            menu.addItem(
                new WatchUi.MenuItem(
                    "No Jumps",
                    "Go ride!",
                    "empty",
                    {}
                )
            );
        }
        
        WatchUi.pushView(menu, new JumpHistoryMenuDelegate(), WatchUi.SLIDE_LEFT);
    }
}
