import math

class AltitudeKalmanFilter:
    def __init__(self, process_noise=0.01, measurement_noise=2.0):
        self.process_noise = process_noise
        self.measurement_noise = measurement_noise
        self.altitude = 0.0
        self.variance = 1.0
        self.is_initialized = False

    def reset(self, initial_altitude):
        self.altitude = initial_altitude
        self.variance = 1.0
        self.is_initialized = True

    def update(self, measurement):
        if not self.is_initialized:
            self.reset(measurement)
            return measurement

        predicted_variance = self.variance + self.process_noise
        kalman_gain = predicted_variance / (predicted_variance + self.measurement_noise)
        self.altitude += kalman_gain * (measurement - self.altitude)
        self.variance = (1 - kalman_gain) * predicted_variance
        return self.altitude

def test_kalman(process_noise, measurement_noise):
    print(f"Testing processNoise={process_noise}, measurementNoise={measurement_noise}")
    filter = AltitudeKalmanFilter(process_noise, measurement_noise)
    filter.reset(0.0)

    # test_smooths_surface_chop
    noise_pattern = [0.5, -0.2, 0.4, -0.5, 0.1, -0.4, 0.3, -0.3, 0.2, -0.1, 0.5, -0.5]
    max_deviation = 0.0
    for i in range(1, 101):
        noise = noise_pattern[i % len(noise_pattern)]
        filtered = filter.update(noise)
        if i > 10:
            if abs(filtered) > max_deviation:
                max_deviation = abs(filtered)
    print(f"  Chop max deviation: {max_deviation} <= 0.15? {max_deviation <= 0.15}")

    # test_ignores_wetsuit_squeeze
    filter.reset(0.0)
    for i in range(1, 21):
        filter.update(0.0)
    lowest_point = 0.0
    for i in range(1, 5):
        filtered = filter.update(-8.0)
        if filtered < lowest_point:
            lowest_point = filtered
    for i in range(1, 11):
        filter.update(0.0)
    print(f"  Squeeze lowest point: {lowest_point} >= -1.5? {lowest_point >= -1.5}")

    # test_tracks_true_jump
    filter.reset(0.0)
    num_readings = 50
    max_filtered_altitude = 0.0
    noise_pattern2 = [0.2, -0.1, -0.2, 0.1, 0.0, 0.2, -0.2]
    # pre-feed realistic baseline
    for i in range(10): filter.update(0.0)
    for i in range(num_readings + 1):
        a = 12.0 / (25 * 25)
        true_altitude = a * i * (num_readings - i)
        noise = noise_pattern2[i % len(noise_pattern2)]
        reading = true_altitude + noise
        filtered = filter.update(reading)
        if filtered > max_filtered_altitude:
            max_filtered_altitude = filtered
    print(f"  Jump max altitude: {max_filtered_altitude} close to 12.0? {abs(max_filtered_altitude - 12.0) <= 0.5}")

    passed = (max_deviation <= 0.15) and (lowest_point >= -1.5) and (abs(max_filtered_altitude - 12.0) <= 0.5)
    return passed

configs = [
    (0.01, 2.0),
    (0.05, 2.0),
    (0.1, 2.0),
    (0.5, 2.0),
    (0.1, 5.0),
    (0.2, 3.0),
    (0.5, 3.0),
    (0.8, 3.0),
    (1.0, 3.0),
    (0.5, 4.0),
    (0.3, 1.5),
    (0.2, 1.0)
]

for p, m in configs:
    if test_kalman(p, m):
        print(f"==> FOUND PASSING PARAMS: processNoise={p}, measurementNoise={m}")
        break
