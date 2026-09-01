package com.zerostress.manager.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun WeeklyLeaderboardScreen(appViewModel: AppViewModel) {
    val dailyLogs by appViewModel.dailyLogs.collectAsState()

    val weeklyStats = dailyLogs.groupBy { it.playerName }.map { (name, logs) ->
        val m = logs.sumOf { it.matches }; val w = logs.sumOf { it.wins }; val k = logs.sumOf { it.kills }
        val d = logs.sumOf { it.damage }
        val score = (k * 10.0) + (d / 100.0) + (w * 50.0)
        val wr = if (m > 0) (w.toDouble() / m) * 100 else 0.0
        val avgK = if (m > 0) k.toDouble() / m else 0.0
        val avgD = if (m > 0) d.toDouble() / m else 0.0
        LeaderboardEntry(name, m, w, k, 0, d, avgD, wr, avgK, score)
    }.sortedByDescending { it.score }.take(10)

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Top 10 Weekly Leaderboard", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        if (weeklyStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No weekly data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn {
            itemsIndexed(weeklyStats) { index, entry ->
                val rankEmoji = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}" }
                val bgColor = when (index) { 0 -> Rank1; 1 -> Rank2; 2 -> Rank3; else -> MaterialTheme.colorScheme.surface }

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(rankEmoji, fontSize = 24.sp, modifier = Modifier.width(40.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${entry.matches}M | ${entry.wins}W | ${entry.kills}K | ${entry.damage}DMG", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${entry.score.toInt()} pts", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Accent)
                            Text("${String.format("%.1f", entry.winRate)}% WR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
