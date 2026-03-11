package com.tracker.gps.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

enum class JumpSensitivity(val takeoffGForce: Float, val minHeight: Double) {
    BAJA(18.0f, 1.5),
    MEDIA(15.0f, 1.0),
    ALTA(12.0f, 0.5)
}

/**
 * Detects jumps based on sensor fusion of Barometer, Rotation Vector, and Linear Acceleration.
 *
 * Detection Logic:
 * 1. Takeoff: Vertical G-force spike.
 * 2. In-Air: Max Altitude tracking.
 * 3. Landing: Negative impact spike.
 * 4. Completion: Check hangtime and min height.
 */
class JumpDetector(
    context: Context,
    private val onJumpDetected: (height: Double, hangtime: Long) -> Unit,
    private val onAltitudeUpdate: (altitude: Double, isJumping: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val kalmanFilter = AltitudeKalmanFilter()
    var sensitivity = JumpSensitivity.MEDIA

    private var currentPressure: Float = 0f
    private var baselineAltitude: Double = 0.0
    private var maxAltitude: Double = 0.0
    private var takeoffTime: Long = 0L
    private var isJumping: Boolean = false

    private val rotationMatrix = FloatArray(9)
    private val linearAcc = FloatArray(3)

    fun start() {
        Log.d("JumpDetector", "Starting JumpDetector")
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun stop() {
        Log.d("JumpDetector", "Stopping JumpDetector")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0]
                // Convert pressure to altitude (standard barometric formula)
                val rawAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure).toDouble()
                val filteredAltitude = kalmanFilter.update(rawAltitude)

                if (isJumping) {
                    if (filteredAltitude > maxAltitude) {
                        maxAltitude = filteredAltitude
                    }
                    onAltitudeUpdate(Math.max(0.0, maxAltitude - baselineAltitude), true)
                } else {
                    onAltitudeUpdate(0.0, false)
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                linearAcc[0] = event.values[0]
                linearAcc[1] = event.values[1]
                linearAcc[2] = event.values[2]

                // Transform linear acceleration into world coordinates to get Earth-Z vertical acceleration
                // a_world = R * a_device
                // R is 3x3 matrix from getRotationMatrixFromVector
                // Vertical acceleration corresponds to world-Z (index 6, 7, 8 of the rotation matrix)
                val verticalAcc = (rotationMatrix[6] * linearAcc[0] +
                                   rotationMatrix[7] * linearAcc[1] +
                                   rotationMatrix[8] * linearAcc[2])

                processAcceleration(verticalAcc)
            }
        }
    }

    private fun processAcceleration(verticalAcc: Float) {
        val currentTime = System.currentTimeMillis()

        if (!isJumping) {
            // Takeoff Detection
            if (verticalAcc > sensitivity.takeoffGForce) {
                isJumping = true
                takeoffTime = currentTime
                baselineAltitude = kalmanFilter.getCurrentAltitude()
                maxAltitude = baselineAltitude
                Log.d("JumpDetector", "Takeoff detected! Baseline: $baselineAltitude")
            }
        } else {
            // Landing Detection
            // Water impacts are messy, so we look for a massive overall acceleration spike (> 15 m/s²)
            // rather than strictly a negative vertical one, as the phone might be tumbling.
            val accMagnitude = Math.sqrt((linearAcc[0] * linearAcc[0] + linearAcc[1] * linearAcc[1] + linearAcc[2] * linearAcc[2]).toDouble()).toFloat()
            
            if (accMagnitude > 15.0f && (currentTime - takeoffTime) > 800) {
                completeJump(currentTime)
            } else if (currentTime - takeoffTime > 10000) { // Safety timeout 10s
                completeJump(currentTime)
            }
        }
    }

    private fun completeJump(landingTime: Long) {
        if (!isJumping) return

        val hangtimeMs = landingTime - takeoffTime
        val height = maxAltitude - baselineAltitude

        Log.d("JumpDetector", "Landing detected! Hangtime: ${hangtimeMs}ms, Height: ${height}m")

        if (hangtimeMs > 1500 && height >= sensitivity.minHeight) {
            onJumpDetected(height, hangtimeMs)
        }

        isJumping = false
        maxAltitude = 0.0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
