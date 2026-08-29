package com.example.gasmonitorapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

// ---------- Palette ----------
private val BgTop = Color(0xFF141A2E)
private val BgBottom = Color(0xFF0C0F1C)
private val Surface1 = Color(0xFF1E2740)
private val TextPrimary = Color(0xFFEDEFF7)
private val TextSecondary = Color(0xFF8C93AC)
private val Flame1 = Color(0xFFC97B4A)
private val Safe = Color(0xFF5FAF9A)
private val Warn = Color(0xFFD9A441)
private val Danger = Color(0xFFE5484D)
private val Track = Color(0xFF2A3350)

@Composable
fun GasMonitorScreen(
    weight: Double,
    gasPpm: Int,
    daysRemainingRegression: Double,
    daysRemainingSarima: Double,
    leakDetected: Boolean,
    onRefresh: () -> Unit = {},
    fullWeightKg: Double = 12.5,
    emptyWeightKg: Double = 1.5,
    safeGasPpm: Int = 400
) {
    val fraction = ((weight - emptyWeightKg) / (fullWeightKg - emptyWeightKg))
        .coerceIn(0.0, 1.0).toFloat()
    val percent = (fraction * 100).toInt()

    val statusColor = when {
        leakDetected -> Danger
        daysRemainingSarima < 2 -> Warn
        else -> Safe
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "SENTINEIX · CYLINDER 01",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Gas Monitor",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                RefreshButton(onRefresh)
            }

            Spacer(Modifier.height(20.dp))

            if (leakDetected) {
                LeakBanner()
                Spacer(Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                GaugeArc(fraction = fraction, statusColor = statusColor)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%.1f".format(daysRemainingSarima),
                        color = TextPrimary,
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    Text("DAYS LEFT", color = TextSecondary, fontSize = 12.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$percent% full",
                        color = statusColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ReadoutCard(
                    label = "WEIGHT",
                    value = "%.2f".format(weight),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
                ReadoutCard(
                    label = "GAS SENSOR",
                    value = gasPpm.toString(),
                    unit = "ppm",
                    dotColor = if (gasPpm > safeGasPpm) Danger else Safe,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Surface(color = Surface1, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "FORECAST COMPARISON",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ForecastColumn(
                            label = "Stastical Baseline",
                            value = daysRemainingRegression,
                            accent = TextSecondary,
                            caption = "Based on your average gas use over the last few days.",
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).height(64.dp).background(Track))
                        ForecastColumn(
                            label = "AI Prediction",
                            value = daysRemainingSarima,
                            accent = Flame1,
                            caption = "Learns your daily cooking pattern for a sharper prediction.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Updated automatically every few minutes",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface1)
            .clickable(onClick = onRefresh),
        contentAlignment = Alignment.Center
    ) {
        Text("↻", color = TextPrimary, fontSize = 20.sp)
    }
}

@Composable
private fun LeakBanner() {
    Surface(color = Danger.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Danger))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Gas leak detected", color = Danger, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Ventilate the area and check the valve now", color = TextPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GaugeArc(fraction: Float, statusColor: Color) {
    Canvas(modifier = Modifier.size(220.dp)) {
        val stroke = 16.dp.toPx()
        val startAngle = 135f
        val maxSweep = 270f
        val diameter = min(size.width, size.height) - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Track,
            startAngle = startAngle,
            sweepAngle = maxSweep,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(Flame1, statusColor, statusColor)),
            startAngle = startAngle,
            sweepAngle = maxSweep * fraction,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )
    }
}

@Composable
private fun ReadoutCard(label: String, value: String, unit: String, modifier: Modifier = Modifier, dotColor: Color? = null) {
    Surface(color = Surface1, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                if (dotColor != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = TextPrimary, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text(unit, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ForecastColumn(label: String, value: Double, accent: Color, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("%.1f".format(value), color = accent, fontSize = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("days", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(caption, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF171310, name = "Normal state")
@Composable
fun GasMonitorScreenPreview() {
    GasMonitorScreen(
        weight = 8.4,
        gasPpm = 120,
        daysRemainingRegression = 4.2,
        daysRemainingSarima = 3.8,
        leakDetected = false
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF171310, name = "Leak alert state")
@Composable
fun GasMonitorScreenLeakPreview() {
    GasMonitorScreen(
        weight = 6.1,
        gasPpm = 950,
        daysRemainingRegression = 2.0,
        daysRemainingSarima = 1.7,
        leakDetected = true
    )
}