package com.zerostress.manager.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.DailyLog
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun DailyLeaderboardScreen(appViewModel: AppViewModel) {
    val dailyLogs by appViewModel.dailyLogs.collectAsState()

    // Aggregate by player
    val playerStats = dailyLogs.groupBy { it.playerName }.map { (name, logs) ->
        val totalMatches = logs.sumOf { it.matches }
        val totalWins = logs.sumOf { it.wins }
        val totalKills = logs.sumOf { it.kills }
        val totalAssists = logs.sumOf { it.assists }
        val totalDamage = logs.sumOf { it.damage }
        val score = (totalKills * 10.0) + (totalDamage / 100.0) + (totalWins * 50.0)
        val winRate = if (totalMatches > 0) (totalWins.toDouble() / totalMatches) * 100 else 0.0
        val avgKills = if (totalMatches > 0) totalKills.toDouble() / totalMatches else 0.0
        val avgDamage = if (totalMatches > 0) totalDamage.toDouble() / totalMatches else 0.0
        LeaderboardEntry(name, totalMatches, totalWins, totalKills, totalAssists, totalDamage, avgDamage, winRate, avgKills, score)
    }.sortedByDescending { it.score }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Today's Top Players", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Text("Excludes Assists from scoring", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        if (playerStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No data yet. Admin must add daily entries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn {
            itemsIndexed(playerStats) { index, entry ->
                val rankEmoji = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}" }
                val bgColor = when (index) { 0 -> Rank1; 1 -> Rank2; 2 -> Rank3; else -> MaterialTheme.colorScheme.surface }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rankEmoji, fontSize = 24.sp, modifier = Modifier.width(40.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${entry.matches}M | ${entry.wins}W | ${entry.kills}K | ${entry.damage}DMG", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${entry.score.toInt()} pts", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Accent)
                            Text("${String.format("%.1f", entry.winRate)}% WR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%.1f", entry.avgKills)} avg K", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

data class LeaderboardEntry(
    val name: String, val matches: Int, val wins: Int, val kills: Int, val assists: Int,
    val damage: Int, val avgDamage: Double, val winRate: Double, val avgKills: Double, val score: Double
)
