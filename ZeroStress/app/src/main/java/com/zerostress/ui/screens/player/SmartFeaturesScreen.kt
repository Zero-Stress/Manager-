package com.zerostress.ui.screens.player

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.ZSBlue
import com.zerostress.ui.theme.ZSGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFeaturesScreen(
    players: List<Player>,
    leaderboard: List<LeaderboardEntry>,
    dailyLogs: List<MatchRecord>,
    insights: List<String>,
    hallOfFame: Map<String, String>,
    onBuildTeam: (Int) -> Unit,
    onGenerateInsights: () -> Unit,
    onGenerateHallOfFame: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🤖 AI Team", "🧠 Insights", "🏆 Hall of Fame")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("⚡ Smart Features") })

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp) })
            }
        }

        when (selectedTab) {
            0 -> AutoTeamBuilder(leaderboard, onBuildTeam)
            1 -> PerformanceInsights(insights, onGenerateInsights)
            2 -> HallOfFameScreen(hallOfFame, onGenerateHallOfFame)
        }
    }
}

@Composable
private fun AutoTeamBuilder(leaderboard: List<LeaderboardEntry>, onBuildTeam: (Int) -> Unit) {
    var squadSize by remember { mutableIntStateOf(4) }
    var builtTeam by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🤖 Auto Team Builder", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Algorithm picks the best squad based on stats", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Squad Size", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4, 5).forEach { size ->
                            FilterChip(selected = squadSize == size, onClick = { squadSize = size },
                                label = { Text("$size") })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        builtTeam = leaderboard.sortedByDescending { it.score }.take(squadSize)
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUILD OPTIMAL TEAM")
                    }
                }
            }
        }

        if (builtTeam.isNotEmpty()) {
            item {
                Text("Built Team", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(builtTeam) { entry ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ZSBlue),
                            contentAlignment = Alignment.Center) {
                            Text(entry.playerName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.playerName, fontWeight = FontWeight.Medium)
                            Text("Score: ${entry.score} | K/D: ${"%.1f".format(entry.avgKills)} | WR: ${"%.0f".format(entry.winRate)}%",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("⭐ ${entry.score}", color = ZSGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = ZSBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Win Prediction", fontWeight = FontWeight.Bold)
                        Text("Select 2 players in Hall of Fame to compare", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceInsights(insights: List<String>, onGenerateInsights: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onGenerateInsights, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, null); Spacer(modifier = Modifier.width(8.dp))
                Text("GENERATE INSIGHTS")
            }
        }

        if (insights.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧠", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No insights yet", fontWeight = FontWeight.Bold)
                        Text("Tap above to analyze your performance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        items(insights) { insight ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(insight, modifier = Modifier.weight(1f), lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun HallOfFameScreen(hallOfFame: Map<String, String>, onGenerate: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.EmojiEvents, null); Spacer(modifier = Modifier.width(8.dp))
                Text("VIEW HALL OF FAME")
            }
        }

        if (hallOfFame.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No records yet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(hallOfFame.entries.toList()) { (title, name) ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 18.sp, modifier = Modifier.width(140.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ZSBlue),
                        contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
