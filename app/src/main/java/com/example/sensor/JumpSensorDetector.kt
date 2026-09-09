package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

sealed class JumpState {
    object Idle : JumpState()
    data class Calibrating(val secondsLeft: Int) : JumpState()
    object ReadyToJump : JumpState()
    data class InFlight(val startTimeMs: Long, val currentFlightMs: Long) : JumpState()
    data class Landed(val heightMeters: Float, val flightTimeMs: Long) : JumpState()
    data class Error(val message: String) : JumpState()
}

class JumpSensorDetector(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _jumpState = MutableStateFlow<JumpState>(JumpState.Idle)
    val jumpState: StateFlow<JumpState> = _jumpState.asStateFlow()

    private val _currentAcceleration = MutableStateFlow(9.8f)
    val currentAcceleration: StateFlow<Float> = _currentAcceleration.asStateFlow()

    private var tStart: Long = 0
    private var isCalibrated = false
    private var pushOffDetected = false
    private var zeroGravitySamplesCount = 0
    private var calibrationJob: Job? = null
    private var flightMonitorJob: Job? = null

    // Constants for detection
    private val GRAVITY = 9.80665f
    private val ZERO_G_THRESHOLD = 3.5f // Acceleration drops close to 0 m/s^2 during free fall
    private val IMPACT_THRESHOLD = 15.0f // Peak acceleration upon landing (> 15 m/s^2)
    private val PUSH_OFF_THRESHOLD = 13.0f // Sharp acceleration increase before takeoff

    fun startMeasurement() {
        calibrationJob?.cancel()
        flightMonitorJob?.cancel()

        isCalibrated = false
        pushOffDetected = false
        zeroGravitySamplesCount = 0
        tStart = 0

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        calibrationJob = scope.launch(Dispatchers.Main) {
            _jumpState.value = JumpState.Calibrating(2)
            delay(1000)
            _jumpState.value = JumpState.Calibrating(1)
            delay(1000)
            isCalibrated = true
            _jumpState.value = JumpState.ReadyToJump

            // 15 seconds timeout to perform jump
            delay(15000)
            if (_jumpState.value == JumpState.ReadyToJump) {
                _jumpState.value = JumpState.Error("Время ожидания прыжка истекло. Попробуйте снова.")
                stopMeasurement()
            }
        }
    }

    fun stopMeasurement() {
        calibrationJob?.cancel()
        flightMonitorJob?.cancel()
        sensorManager.unregisterListener(this)
    }

    fun reset() {
        stopMeasurement()
        _jumpState.value = JumpState.Idle
        _currentAcceleration.value = 9.8f
    }

    /**
     * Allows testing / emulator preview of the algorithm with verified mathematical flight calculation
     */
    fun simulateJump(simulatedHeightMeters: Float = 0.58f) {
        calibrationJob?.cancel()
        flightMonitorJob?.cancel()

        scope.launch(Dispatchers.Main) {
            _jumpState.value = JumpState.Calibrating(2)
            delay(800)
            _jumpState.value = JumpState.Calibrating(1)
            delay(800)
            _jumpState.value = JumpState.ReadyToJump
            delay(400)

            // Flight calculation: h = (g * t^2) / 8 => t = sqrt(8 * h / g)
            val flightSeconds = sqrt(8.0 * simulatedHeightMeters / GRAVITY)
            val flightMs = (flightSeconds * 1000).toLong()

            val startTime = System.currentTimeMillis()
            _jumpState.value = JumpState.InFlight(startTime, 0)

            val steps = 8
            for (i in 1..steps) {
                delay(flightMs / steps)
                _currentAcceleration.value = 1.2f
                _jumpState.value = JumpState.InFlight(startTime, (flightMs * i / steps))
            }

            _currentAcceleration.value = 22.5f // Landing impact
            val finalHeight = (GRAVITY * (flightSeconds * flightSeconds) / 8.0).toFloat()
            _jumpState.value = JumpState.Landed(finalHeight, flightMs)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val totalAcc = sqrt(ax * ax + ay * ay + az * az)
        _currentAcceleration.value = totalAcc

        if (!isCalibrated) return

        when (val state = _jumpState.value) {
            is JumpState.ReadyToJump -> {
                // Step 3: Push-off detection
                if (totalAcc > PUSH_OFF_THRESHOLD || ay > 12f) {
                    pushOffDetected = true
                }

                // Step 4: Flight detection (approaching 0 m/s^2)
                if (totalAcc < ZERO_G_THRESHOLD) {
                    tStart = System.currentTimeMillis()
                    zeroGravitySamplesCount = 1
                    _jumpState.value = JumpState.InFlight(tStart, 0)

                    flightMonitorJob = scope.launch(Dispatchers.Main) {
                        while (true) {
                            delay(50)
                            val now = System.currentTimeMillis()
                            val curFlight = now - tStart
                            if (_jumpState.value is JumpState.InFlight) {
                                _jumpState.value = JumpState.InFlight(tStart, curFlight)
                            } else {
                                break
                            }
                            // Safety max flight limit (1.6s ~ 3.1m)
                            if (curFlight > 1600) {
                                _jumpState.value = JumpState.Error("Превышено максимальное время полета")
                                stopMeasurement()
                                break
                            }
                        }
                    }
                }
            }

            is JumpState.InFlight -> {
                if (totalAcc < ZERO_G_THRESHOLD) {
                    zeroGravitySamplesCount++
                }

                // Step 5: Landing detection (impact spike > 15 m/s^2)
                if (totalAcc > IMPACT_THRESHOLD) {
                    flightMonitorJob?.cancel()
                    val tEnd = System.currentTimeMillis()
                    val deltaMs = tEnd - tStart
                    val deltaSec = deltaMs / 1000.0f

                    // Anti-cheat verification:
                    // 1. Must have had real free fall (at least ~120ms of low acceleration)
                    // Shaking the phone creates rapid oscillation without sustained zero-g
                    if (deltaMs < 120 || zeroGravitySamplesCount < 2) {
                        _jumpState.value = JumpState.Error("Античит: обнаружено встряхивание рукой без свободного падения!")
                        stopMeasurement()
                        return
                    }

                    // Step 6: Calculate Height = (g * delta_t^2) / 8
                    val height = (GRAVITY * deltaSec * deltaSec) / 8.0f

                    // Step 7: Filter: Height < 0.1m or > 3.0m
                    if (height < 0.1f) {
                        _jumpState.value = JumpState.Error("Слишком низкий прыжок (< 10 см). Попробуйте еще раз.")
                    } else if (height > 3.0f) {
                        _jumpState.value = JumpState.Error("Ошибка измерения: результат более 3 метров аннулирован.")
                    } else {
                        _jumpState.value = JumpState.Landed(height, deltaMs)
                    }
                    stopMeasurement()
                }
            }

            else -> Unit
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
