package com.zerostress.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.*

@Composable
fun ProfileScreen(
    player: Player,
    dailyLogs: List<MatchRecord>,
    leaderboard: List<LeaderboardEntry>,
    onLogout: () -> Unit
) {
    val myLogs = dailyLogs.filter { it.playerName == player.name }
    val myEntry = leaderboard.find { it.playerName == player.name }

    val totalMatches = myLogs.sumOf { it.matches }
    val totalWins = myLogs.sumOf { it.wins }
    val totalKills = myLogs.sumOf { it.kills }
    val totalAssists = myLogs.sumOf { it.assists }
    val totalDamage = myLogs.sumOf { it.damage }
    val winRate = if (totalMatches > 0) (totalWins.toDouble() / totalMatches) * 100 else 0.0
    val avgDamage = if (totalMatches > 0) totalDamage / totalMatches else 0
    val score = myEntry?.score ?: MatchRecord.calculateScore(totalKills, totalDamage, totalWins)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(player.name.take(1).uppercase(), fontSize = 32.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(player.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("+880 ${player.phone}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(player.role.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }

        // Stats Grid
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 Lifetime Stats", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Matches", "$totalMatches", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("Wins", "$totalWins", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                StatCard("Win Rate", "${String.format("%.1f", winRate)}%", Gold, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Kills", "$totalKills", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                StatCard("Assists", "$totalAssists", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                StatCard("Damage", "$totalDamage", Gold, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Avg Damage", "$avgDamage", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("Score", "$score pts", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("Rank", "#${leaderboard.indexOf(myEntry) + 1}", Gold, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, accentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, color = accentColor, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
