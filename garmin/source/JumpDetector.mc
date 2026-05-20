using Toybox.Sensor;
using Toybox.Math;
using Toybox.System;

class JumpDetector {
    private var kalmanFilter;
    
    // Jump states
    private var isJumping = false;
    private var baselineAltitude = 0.0f;
    private var maxAltitude = 0.0f;
    private var takeoffTime = 0;
    private var lastJumpTime = 0;
    
    // Config
    private var takeoffGForce = 22.0f; // media
    private var minHeight = 1.0f; // media
    private var JUMP_COOLDOWN_MS = 2000;
    
    // Callback targets
    private var parentApp;

    function initialize(app) {
        parentApp = app;
        kalmanFilter = new AltitudeKalmanFilter(0.01f, 2.0f); // Default values
    }

    function start() {
        // Enable sensors for altitude and accelerometer
        Sensor.enableSensorEvents(method(:onSensor));
        System.println("JumpDetector started");
    }

    function stop() {
        Sensor.enableSensorEvents(null);
    }

    function onSensor(info as Sensor.Info) as Void {
        var currentTime = System.getTimer();
        
        // Update altitude if available
        if (info.altitude != null) {
            var filteredAltitude = kalmanFilter.update(info.altitude.toFloat());
            
            if (isJumping) {
                if (filteredAltitude > maxAltitude) {
                    maxAltitude = filteredAltitude;
                }
                var currentHeight = maxAltitude - baselineAltitude;
                if (currentHeight < 0) { currentHeight = 0.0f; }
                
                // You could report current jump height here if needed
            }
        }
        
        // Acceleration check (if available) - requires SENSOR_ACCELEROMETER or similar,
        // but watch accel is typically in Sensor.Info.accel (Array of [x,y,z] in m/s^2)
        if (info.accel != null) {
            var accRaw = info.accel;
            if (accRaw instanceof Toybox.Lang.Array) {
                var accel = accRaw as Toybox.Lang.Array<Toybox.Lang.Float>;
                var ax = accel[0];
                var ay = accel[1];
                var az = accel[2];
                var accMagnitude = Math.sqrt(ax*ax + ay*ay + az*az);
            
            if (!isJumping) {
                // Takeoff Detection
                if (accMagnitude > takeoffGForce && (currentTime - lastJumpTime > JUMP_COOLDOWN_MS)) {
                    isJumping = true;
                    takeoffTime = currentTime;
                    baselineAltitude = kalmanFilter.update(info.altitude != null ? info.altitude.toFloat() : 0.0f);
                    maxAltitude = baselineAltitude;
                    System.println("Takeoff detected");
                }
            } else {
                // Landing Detection
                if (accMagnitude > 15.0f && (currentTime - takeoffTime) > 800) {
                    completeJump(currentTime);
                } else if (currentTime - takeoffTime > 10000) {
                    // Safety timeout 10s
                    completeJump(currentTime);
                }
            }
            } // Close if instance
        }
    }

    private function completeJump(landingTime) {
        if (!isJumping) { return; }
        
        var hangtimeMs = landingTime - takeoffTime;
        var height = maxAltitude - baselineAltitude;
        
        System.println("Landing detected! Hangtime: " + hangtimeMs + "ms, Height: " + height + "m");
        
        if (hangtimeMs > 1500 && height >= minHeight) {
            parentApp.onJumpDetected(height, hangtimeMs);
        }
        
        isJumping = false;
        maxAltitude = 0.0f;
        lastJumpTime = System.getTimer();
    }
}
