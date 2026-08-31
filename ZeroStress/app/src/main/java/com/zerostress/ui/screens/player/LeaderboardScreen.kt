package com.zerostress.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.zerostress.data.model.LeaderboardEntry
import com.zerostress.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    leaderboard: List<LeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Daily", "Weekly")

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text("🏆 Leaderboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Top players ranked by score", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
            }
        }

        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No rankings yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Play matches to appear here", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(leaderboard.take(20)) { index, entry ->
                    LeaderboardCard(index = index, entry = entry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(index: Int, entry: LeaderboardEntry) {
    val medalColor = when (index) {
        0 -> Gold
        1 -> Silver
        2 -> Bronze
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val medal = when (index) {
        0 -> "👑"
        1 -> "🥈"
        2 -> "🥉"
        else -> "#${index + 1}"
    }
    val bgColor = when (index) {
        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
        2 -> Bronze.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (index < 3) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank/Medal
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(medal, fontSize = if (index < 3) 20.sp else 12.sp, fontWeight = FontWeight.Bold,
                    color = medalColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.playerName, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    if (entry.isOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OnlineGreen))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "K:${entry.kills}  A:${entry.assists}  D:${entry.damage}  W:${entry.wins}  |  ${String.format("%.1f", entry.winRate)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.score}", fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Text("pts", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}
