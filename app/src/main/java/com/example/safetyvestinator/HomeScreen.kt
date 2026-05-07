package com.example.safetyvestinator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safetyvestinator.data.SensorReading
import com.example.safetyvestinator.viewmodel.BleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.example.safetyvestinator.data.GpsLocation
import com.example.safetyvestinator.ui.VestMapView

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    bleViewModel: BleViewModel,
    debugMode: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    }
    val dateLabel = today.format(formatter)

    val readings by bleViewModel.recentReadings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (debugMode) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DEBUG MODE - Impact Threshold Lowered",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = buildAnnotatedString {
                append("Hi! It is ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(dateLabel)
                }
            },
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Most Recent Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (readings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data yet. Connect a device in Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AccelerationChart(
                        readings = readings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    LatestReadingRow(readings.last())
                }
            }
        }

        val location by bleViewModel.location.collectAsStateWithLifecycle()
        LocationCard(location = location)
    }
}

@Composable
private fun AccelerationChart(
    readings: List<SensorReading>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(readings) {
        if (readings.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(readings.map { it.ax.toDouble() })
                series(readings.map { it.ay.toDouble() })
                series(readings.map { it.az.toDouble() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false)
    )
}

@Composable
private fun LatestReadingRow(reading: SensorReading) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Accel: %.2f, %.2f, %.2f m/s²".format(reading.ax, reading.ay, reading.az),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Gyro:  %.2f, %.2f, %.2f rad/s".format(reading.gx, reading.gy, reading.gz),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Temp:  %.1f °F".format(reading.tempF),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LocationCard(location: GpsLocation?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                VestMapView(
                    location = location,
                    modifier = Modifier.fillMaxSize()
                )
                if (location == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Waiting for GPS fix…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (location != null) {
                Text(
                    text = "%.6f, %.6f".format(location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodyMedium
                )
                val ageMs = System.currentTimeMillis() - location.receivedAtMillis
                val ageText = when {
                    ageMs < 5_000 -> "just now"
                    ageMs < 60_000 -> "${ageMs / 1000}s ago"
                    ageMs < 3_600_000 -> "${ageMs / 60_000}m ago"
                    else -> "${ageMs / 3_600_000}h ago"
                }
                Text(
                    text = "Updated $ageText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}