class AltitudeKalmanFilter {
    constructor(processNoise = 0.01, measurementNoise = 2.0) {
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
        this.altitude = 0.0;
        this.variance = 1.0;
        this.isInitialized = false;
    }

    reset(initialAltitude) {
        this.altitude = initialAltitude;
        this.variance = 1.0;
        this.isInitialized = true;
    }

    update(measurement) {
        if (!this.isInitialized) {
            this.reset(measurement);
            return measurement;
        }

        const residual = Math.abs(measurement - this.altitude);
        const adaptiveMeasurementNoise = residual > 3.0 ? this.measurementNoise * 20 : this.measurementNoise;

        const predictedVariance = this.variance + this.processNoise;
        const kalmanGain = predictedVariance / (predictedVariance + adaptiveMeasurementNoise);
        this.altitude += kalmanGain * (measurement - this.altitude);
        this.variance = (1 - kalmanGain) * predictedVariance;
        return this.altitude;
    }
}

const processNoises = [0.1, 0.2, 0.3, 0.5];
const measurementNoises = [1.0, 2.0, 3.0];

for (let p of processNoises) {
    for (let m of measurementNoises) {
        let filter = new AltitudeKalmanFilter(p, m);
        filter.reset(0.0);

        // test_smooths_surface_chop
        const noisePattern = [0.5, -0.2, 0.4, -0.5, 0.1, -0.4, 0.3, -0.3, 0.2, -0.1, 0.5, -0.5];
        let maxDeviation = 0.0;
        for (let i = 1; i <= 100; i++) {
            const noise = noisePattern[i % noisePattern.length];
            const filtered = filter.update(noise);
            if (i > 10) {
                if (Math.abs(filtered) > maxDeviation) maxDeviation = Math.abs(filtered);
            }
        }

        // test_ignores_wetsuit_squeeze
        filter.reset(0.0);
        for (let i = 1; i <= 20; i++) filter.update(0.0);
        let lowestPoint = 0.0;
        for (let i = 1; i <= 4; i++) {
            const filtered = filter.update(-8.0);
            if (filtered < lowestPoint) lowestPoint = filtered;
        }

        // test_tracks_true_jump
        filter.reset(0.0);
        const numReadings = 50;
        let maxFilteredAltitude = 0.0;
        const noisePattern2 = [0.2, -0.1, -0.2, 0.1, 0.0, 0.2, -0.2];
        for (let i = 0; i < 10; i++) filter.update(0.0);
        for (let i = 0; i <= numReadings; i++) {
            const a = 12.0 / (25 * 25);
            const trueAltitude = a * i * (numReadings - i);
            const noise = noisePattern2[i % noisePattern2.length];
            const reading = trueAltitude + noise;
            const filtered = filter.update(reading);
            if (filtered > maxFilteredAltitude) maxFilteredAltitude = filtered;
        }

        console.log(`P=${p}, M=${m} -> Chop: ${maxDeviation.toFixed(3)} (<=0.15), Squeeze: ${lowestPoint.toFixed(3)} (>=-1.5), Jump: ${maxFilteredAltitude.toFixed(3)} (11.5-12.5)`);
    }
}
