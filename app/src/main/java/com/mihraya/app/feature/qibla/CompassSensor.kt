package com.mihraya.app.feature.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassReading(
    /** Degrees clockwise from magnetic north, smoothed. */
    val azimuth: Float,
    val accuracy: Int,
    /** True when the phone is tilted too far from flat for a reliable heading. */
    val tilted: Boolean,
)

class CompassSensor(context: Context) {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val rotationVector = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean = rotationVector != null || (accelerometer != null && magnetometer != null)

    fun readings(displayRotation: () -> Int): Flow<CompassReading> = callbackFlow {
        val sensors = manager ?: run {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnet = false
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        var sinSmooth = 0f
        var cosSmooth = 1f
        var initialised = false

        fun emitFrom(matrix: FloatArray) {
            val (axisX, axisY) = when (displayRotation()) {
                Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            }
            SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remapped)
            SensorManager.getOrientation(remapped, orientation)
            val azimuth = orientation[0]
            val pitch = Math.toDegrees(orientation[1].toDouble())
            val roll = Math.toDegrees(orientation[2].toDouble())
            if (!initialised) {
                sinSmooth = sin(azimuth)
                cosSmooth = cos(azimuth)
                initialised = true
            } else {
                sinSmooth += SMOOTHING * (sin(azimuth) - sinSmooth)
                cosSmooth += SMOOTHING * (cos(azimuth) - cosSmooth)
            }
            val degrees = ((Math.toDegrees(atan2(sinSmooth, cosSmooth).toDouble()) + 360.0) % 360.0).toFloat()
            trySend(CompassReading(degrees, accuracy, tilted = abs(pitch) > 55 || abs(roll) > 55))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotation, event.values)
                        emitFrom(rotation)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPass(event.values, geomagnetic)
                        hasMagnet = true
                        if (rotationVector == null && hasGravity &&
                            SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
                        ) {
                            emitFrom(rotation)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = newAccuracy
            }
        }

        if (rotationVector != null) {
            sensors.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            magnetometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        } else {
            accelerometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        awaitClose { sensors.unregisterListener(listener) }
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        for (i in output.indices) output[i] += 0.2f * (input[i] - output[i])
    }

    private companion object {
        const val SMOOTHING = 0.18f
    }
}
