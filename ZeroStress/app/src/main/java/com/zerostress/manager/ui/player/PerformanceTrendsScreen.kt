package com.zerostress.manager.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.DailyLog
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun PerformanceTrendsScreen(phone: String, appViewModel: AppViewModel) {
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    val playerLogs = dailyLogs.filter { it.phone == phone }.sortedBy { it.timestamp }

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Performance Trends", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Text("Track your improvement over time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (playerLogs.size < 2) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Need at least 2 entries to show trends.\nKeep playing!", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
        } else {
            // Score trend
            TrendChart("Score Over Time", playerLogs.map { it.calculateScore() }, Accent)
            Spacer(modifier = Modifier.height(16.dp))

            // Kills trend
            TrendChart("Kills Per Entry", playerLogs.map { it.kills.toDouble() }, Success)
            Spacer(modifier = Modifier.height(16.dp))

            // Damage trend
            TrendChart("Damage Per Entry", playerLogs.map { it.damage.toDouble() }, Warning)
            Spacer(modifier = Modifier.height(16.dp))

            // Win Rate trend
            TrendChart("Win Rate Trend", playerLogs.map {
                if (it.matches > 0) (it.wins.toDouble() / it.matches) * 100 else 0.0
            }, Gold)
            Spacer(modifier = Modifier.height(16.dp))

            // Summary card
            val latest = playerLogs.last()
            val previous = if (playerLogs.size >= 2) playerLogs[playerLogs.size - 2] else latest
            val scoreDiff = latest.calculateScore() - previous.calculateScore()
            val killsDiff = latest.kills - previous.kills

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Latest Change", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TrendIndicator("Score", scoreDiff, "+${scoreDiff.toInt()} pts")
                        TrendIndicator("Kills", killsDiff.toDouble(), "+$killsDiff")
                        TrendIndicator("Damage", (latest.damage - previous.damage).toDouble(), "+${latest.damage - previous.damage}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendIndicator(label: String, diff: Double, defaultText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        val color = when { diff > 0 -> Success; diff < 0 -> Danger; else -> MaterialTheme.colorScheme.onSurfaceVariant }
        val text = when { diff > 0 -> "↑ $defaultText"; diff < 0 -> "↓ ${String.format("%.0f", diff)}"; else -> "— 0" }
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
    }
}

@Composable
fun TrendChart(title: String, values: List<Double>, color: Color) {
    if (values.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            Spacer(modifier = Modifier.height(8.dp))

            val maxVal = values.maxOrNull() ?: 1.0
            val minVal = values.minOrNull() ?: 0.0
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                val stepX = if (values.size > 1) w / (values.size - 1) else w

                // Draw line
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = h - ((v - minVal) / range * h).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 3f))

                // Draw dots
                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = h - ((v - minVal) / range * h).toFloat()
                    drawCircle(color, radius = 5f, center = Offset(x, y))
                }
            }

            // Min/Max labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Low: ${String.format("%.0f", minVal)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("High: ${String.format("%.0f", maxVal)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
