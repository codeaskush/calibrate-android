package com.kushagra.calibrate

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LevelScreen()
                }
            }
        }
    }
}

// Turns an accelerometer reading (m/s²) into a tilt angle in degrees
fun toDegrees(value: Float): Float {
    val ratio = (value / 9.81f).coerceIn(-1f, 1f)
    return Math.toDegrees(asin(ratio.toDouble())).toFloat()
}

@Composable
fun LevelScreen() {
    val context = LocalContext.current

    // Live tilt from the sensor
    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }

    // Saved "zero" point when the user taps Calibrate
    var zeroX by remember { mutableFloatStateOf(0f) }
    var zeroY by remember { mutableFloatStateOf(0f) }

    // Start listening to the accelerometer, stop when the screen closes
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                rawX = event.values[0]
                rawY = event.values[1]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val x = rawX - zeroX
    val y = rawY - zeroY
    val degX = toDegrees(x)
    val degY = toDegrees(y)
    val isLevel = abs(degX) < 1f && abs(degY) < 1f
    val bubbleColor = if (isLevel) Color(0xFF2E7D32) else Color(0xFFF9A825)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Calibrate", fontSize = 32.sp)
        Text("Lay your phone on a surface", fontSize = 16.sp)

        Spacer(Modifier.height(32.dp))

        // The level: a ring with a bubble that moves to the high side
        Canvas(modifier = Modifier.size(260.dp)) {
            val ringRadius = size.minDimension / 2
            val bubbleRadius = 28.dp.toPx()
            val maxMove = ringRadius - bubbleRadius

            var bx = x / 9.81f * maxMove * 3
            var by = -y / 9.81f * maxMove * 3
            // keep the bubble inside the ring
            val distance = sqrt(bx * bx + by * by)
            if (distance > maxMove) {
                bx = bx / distance * maxMove
                by = by / distance * maxMove
            }

            drawCircle(Color.Gray, radius = ringRadius, style = Stroke(4.dp.toPx()))
            drawCircle(Color.Gray, radius = bubbleRadius + 6.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(bubbleColor, radius = bubbleRadius, center = center + Offset(bx, by))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = if (isLevel) "Level ✓" else "X: %.1f°   Y: %.1f°".format(degX, degY),
            fontSize = 22.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { zeroX = rawX; zeroY = rawY }) { Text("Calibrate") }
            OutlinedButton(onClick = { zeroX = 0f; zeroY = 0f }) { Text("Reset") }
        }
    }
}