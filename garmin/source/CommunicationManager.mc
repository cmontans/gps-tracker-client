using Toybox.Communications;
using Toybox.System;
using Toybox.Lang;

// Manager for phone communication and server sync
class CommunicationManager {

    private var _phoneConnected;
    private var _serverConnected;
    private var _serverUrl;
    private var _userId;
    private var _userName;
    private var _groupName;

    const DEFAULT_SERVER_URL = "wss://gps-tracker-server-production-5900.up.railway.app";

    function initialize() {
        _phoneConnected = false;
        _serverConnected = false;
        _serverUrl = DEFAULT_SERVER_URL;
        _userId = generateUserId();
        _userName = "Garmin User";
        _groupName = "default";
    }

    // Generate a unique user ID
    private function generateUserId() {
        var deviceId = System.getDeviceSettings().uniqueIdentifier;
        return deviceId != null ? deviceId : "garmin_" + System.getTimer().toString();
    }

    // Send speed update to phone
    function sendSpeedUpdate(speed, latitude, longitude, bearing) {
        if (!Communications.isPhoneConnected()) {
            _phoneConnected = false;
            return;
        }

        _phoneConnected = true;

        var params = {
            "type" => "speed_update",
            "userId" => _userId,
            "speed" => speed,
            "latitude" => latitude,
            "longitude" => longitude,
            "bearing" => bearing,
            "timestamp" => System.getTimer()
        };

        Communications.transmit(params, null, new $.CommListener());
    }

    // Send tracking state to phone
    function sendTrackingState(isTracking) {
        if (!Communications.isPhoneConnected()) {
            return;
        }

        var params = {
            "type" => "tracking_state",
            "userId" => _userId,
            "tracking" => isTracking
        };

        Communications.transmit(params, null, new $.CommListener());
    }

    // Send reset stats command
    function sendResetStats() {
        if (!Communications.isPhoneConnected()) {
            return;
        }

        var params = {
            "type" => "reset_stats",
            "userId" => _userId
        };

        Communications.transmit(params, null, new $.CommListener());
    }

    // Send group horn
    function sendGroupHorn() {
        if (!Communications.isPhoneConnected()) {
            return;
        }

        var params = {
            "type" => "group_horn",
            "userId" => _userId
        };

        Communications.transmit(params, null, new $.CommListener());
    }

    // Make HTTP request to server
    function makeWebRequest(endpoint, params, callback) {
        var url = _serverUrl + endpoint;

        var options = {
            :method => Communications.HTTP_REQUEST_METHOD_POST,
            :headers => {
                "Content-Type" => Communications.REQUEST_CONTENT_TYPE_JSON
            },
            :responseType => Communications.HTTP_RESPONSE_CONTENT_TYPE_JSON
        };

        Communications.makeWebRequest(url, params, options, callback);
    }

    // Register user with server
    function registerUser() {
        var params = {
            "type" => "register",
            "userId" => _userId,
            "userName" => _userName,
            "groupName" => _groupName
        };

        makeWebRequest("/register", params, new $.CommListener());
    }

    // Check phone connection
    function isPhoneConnected() {
        _phoneConnected = Communications.isPhoneConnected();
        return _phoneConnected;
    }

    // Getters/Setters
    function setServerUrl(url) {
        _serverUrl = url;
    }

    function setUserName(name) {
        _userName = name;
    }

    function setGroupName(name) {
        _groupName = name;
    }

    function getUserId() {
        return _userId;
    }

    function getUserName() {
        return _userName;
    }

    function getGroupName() {
        return _groupName;
    }

    function isServerConnected() {
        return _serverConnected;
    }
}

// Communication listener for callbacks
class CommListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() {
        System.println("Communication completed");
    }

    function onError() {
        System.println("Communication error");
    }
}
