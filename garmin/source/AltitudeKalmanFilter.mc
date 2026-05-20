import Toybox.Math;
import Toybox.Lang;

class AltitudeKalmanFilter {
    private var processNoise as Lang.Float;
    private var measurementNoise as Lang.Float;
    private var altitude as Lang.Float;
    private var variance as Lang.Float;
    private var isInitialized as Lang.Boolean;

    function initialize(pNoise, mNoise) {
        processNoise = pNoise;
        measurementNoise = mNoise;
        altitude = 0.0f;
        variance = 1.0f;
        isInitialized = false;
    }

    function reset(initialAltitude) {
        altitude = initialAltitude;
        variance = 1.0f;
        isInitialized = true;
    }

    function update(measurement) {
        if (!isInitialized) {
            reset(measurement);
            return measurement;
        }

        var residual = measurement - altitude;
        if (residual < 0) {
            residual = -residual;
        }
        var adaptiveMeasurementNoise = measurementNoise;
        if (residual > 3.0) {
            adaptiveMeasurementNoise = measurementNoise * 20.0f;
        }

        var predictedVariance = variance + processNoise;
        var kalmanGain = predictedVariance / (predictedVariance + adaptiveMeasurementNoise);
        
        altitude = altitude + kalmanGain * (measurement - altitude);
        variance = (1.0f - kalmanGain) * predictedVariance;
        
        return altitude;
    }
}
