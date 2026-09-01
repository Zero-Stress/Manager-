package com.zerostress.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    playerName: String,
    dailyLogs: List<MatchRecord>,
    leaderboard: List<LeaderboardEntry>,
    insights: List<String>,
    onGenerateInsights: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📊 Radar", "🗺️ Trend", "⏱️ Session", "📅 Career")
    val playerLogs = dailyLogs.filter { it.playerName == playerName }
    val playerEntry = leaderboard.find { it.playerName == playerName }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("📊 Analytics") })
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 10.sp) })
            }
        }
        when (selectedTab) {
            0 -> RadarTab(playerEntry)
            1 -> TrendTab(playerLogs, insights, onGenerateInsights)
            2 -> SessionTab(playerLogs)
            3 -> CareerTab(playerLogs, playerEntry)
        }
    }
}

@Composable
private fun RadarTab(entry: LeaderboardEntry?) {
    val maxValues = listOf(50f, 5000f, 30f, 10f, 600f)
    val values = listOf(
        (entry?.kills?.toFloat() ?: 0f).coerceAtMost(maxValues[0]),
        (entry?.damage?.toFloat() ?: 0f).coerceAtMost(maxValues[1]),
        entry?.winRate?.toFloat() ?: 0f,
        entry?.avgKills?.toFloat()?.times(10) ?: 0f,
        (entry?.survivalSeconds?.toFloat() ?: 0f).coerceAtMost(maxValues[4])
    )
    val labels = listOf("Kills", "Damage", "Win Rate%", "K/D", "Survival")
    val normalized = values.mapIndexed { i, v -> (v / maxValues[i]).coerceIn(0f, 1f) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕸️ Skill Radar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Canvas(modifier = Modifier.size(250.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 30
                        val n = labels.size
                        // Draw grid
                        for (level in 1..5) {
                            val path = Path()
                            for (i in 0 until n) {
                                val angle = (Math.PI * 2 * i / n) - Math.PI / 2
                                val r = radius * level / 5
                                val x = center.x + r * kotlin.math.cos(angle).toFloat()
                                val y = center.y + r * kotlin.math.sin(angle).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            path.close()
                            drawPath(path, Color.Gray.copy(alpha = 0.2f), style = Stroke(1.dp.toPx()))
                        }
                        // Draw axes
                        for (i in 0 until n) {
                            val angle = (Math.PI * 2 * i / n) - Math.PI / 2
                            val x = center.x + radius * kotlin.math.cos(angle).toFloat()
                            val y = center.y + radius * kotlin.math.sin(angle).toFloat()
                            drawLine(Color.Gray.copy(alpha = 0.3f), center, Offset(x, y), 1.dp.toPx())
                        }
                        // Draw data
                        val dataPath = Path()
                        for (i in 0 until n) {
                            val angle = (Math.PI * 2 * i / n) - Math.PI / 2
                            val r = radius * normalized[i]
                            val x = center.x + r * kotlin.math.cos(angle).toFloat()
                            val y = center.y + r * kotlin.math.sin(angle).toFloat()
                            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                        }
                        dataPath.close()
                        drawPath(dataPath, ZSBlue.copy(alpha = 0.3f))
                        drawPath(dataPath, ZSBlue, style = Stroke(3.dp.toPx()))
                        // Draw dots
                        for (i in 0 until n) {
                            val angle = (Math.PI * 2 * i / n) - Math.PI / 2
                            val r = radius * normalized[i]
                            val x = center.x + r * kotlin.math.cos(angle).toFloat()
                            val y = center.y + r * kotlin.math.sin(angle).toFloat()
                            drawCircle(ZSBlue, 6.dp.toPx(), Offset(x, y))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        labels.forEachIndexed { i, label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${"%.0f".format(normalized[i] * 100)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ZSBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendTab(logs: List<MatchRecord>, insights: List<String>, onGenerate: () -> Unit) {
    val recentLogs = logs.take(10).reversed()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Analytics, null); Spacer(modifier = Modifier.width(8.dp))
                Text("ANALYZE MY PERFORMANCE")
            }
        }

        if (recentLogs.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📈 Recent Performance", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        // Simple bar chart
                        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            val barWidth = size.width / recentLogs.size - 4
                            val maxScore = recentLogs.maxOfOrNull { MatchRecord.calculateScore(it.kills, it.damage, it.wins) }?.toFloat() ?: 1f
                            recentLogs.forEachIndexed { i, log ->
                                val score = MatchRecord.calculateScore(log.kills, log.damage, log.wins).toFloat()
                                val barHeight = (score / maxScore) * size.height * 0.8f
                                drawRect(ZSBlue.copy(alpha = 0.7f),
                                    Offset(i * (barWidth + 4) + 2, size.height - barHeight),
                                    androidx.compose.ui.geometry.Size(barWidth, barHeight))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Oldest", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Recent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (insights.isNotEmpty()) {
            item { Text("🧠 Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(insights) { insight ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(insight, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionTab(logs: List<MatchRecord>) {
    val totalMinutes = logs.size * 25 // estimate ~25 min per match
    val totalHours = totalMinutes / 60

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱️ Session Stats", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎮", fontSize = 24.sp)
                            Text("${logs.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                            Text("Sessions", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🕐", fontSize = 24.sp)
                            Text("${totalHours}h", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                            Text("Playtime", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2D1B))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Fatigue Detection", fontWeight = FontWeight.Bold, color = ZSGreen)
                        val avgSurvival = if (logs.isNotEmpty()) logs.map { it.survivalSeconds }.average() else 0.0
                        Text(if (avgSurvival > 300) "Your stats are consistent! Keep it up."
                        else "Consider taking breaks between sessions for better performance.", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerTab(logs: List<MatchRecord>, entry: LeaderboardEntry?) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📅 Career Timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val milestones = mutableListOf<Pair<String, String>>()
                    if (logs.isNotEmpty()) milestones.add("🎮 First Match" to "Started your journey")
                    val totalKills = logs.sumOf { it.kills }
                    if (totalKills >= 10) milestones.add("🔫 10 Kill Club" to "Total: $totalKills kills")
                    if (totalKills >= 50) milestones.add("💀 50 Kill Monster" to "Total: $totalKills kills")
                    if (totalKills >= 100) milestones.add("🏆 Century Killer" to "Total: $totalKills kills")
                    val totalWins = logs.sumOf { it.wins }
                    if (totalWins >= 1) milestones.add("🥇 First Win" to "Victory!")
                    if (totalWins >= 10) milestones.add("🔥 10 Win Streak" to "Total: $totalWins wins")
                    if (entry != null && entry.score >= 1000) milestones.add("⭐ Score Master" to "Score: ${entry.score}")
                    if (logs.size >= 10) milestones.add("📊 Data Analyst" to "${logs.size} matches recorded")
                    if (milestones.isEmpty()) milestones.add("🌱 Just Starting" to "Play your first match!")

                    milestones.forEachIndexed { i, (title, desc) ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ZSBlue))
                                if (i < milestones.size - 1) {
                                    Box(modifier = Modifier.width(2.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(title, fontWeight = FontWeight.Bold)
                                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 All-Time Stats", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Total Matches", "${logs.sumOf { it.matches }}")
                    StatRow("Total Wins", "${logs.sumOf { it.wins }}")
                    StatRow("Total Kills", "$totalKills")
                    StatRow("Total Damage", "${logs.sumOf { it.damage }}")
                    StatRow("Best Score", "${logs.maxOfOrNull { MatchRecord.calculateScore(it.kills, it.damage, it.wins) } ?: 0}")
                    StatRow("Avg Kill/Match", "${"%.1f".format(if (logs.isNotEmpty()) logs.map { it.kills }.average() else 0.0)}")
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
