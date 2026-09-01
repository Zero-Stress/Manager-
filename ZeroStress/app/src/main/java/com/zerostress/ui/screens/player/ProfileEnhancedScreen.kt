package com.zerostress.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.zerostress.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEnhancedScreen(
    player: Player,
    dailyLogs: List<MatchRecord>,
    leaderboard: List<LeaderboardEntry>,
    coinBalance: ZSCoinBalance?,
    titles: List<PlayerTitle>,
    achievements: List<Achievement>,
    capsules: List<TimeCapsule>,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("👤 Profile", "🏷️ Titles", "🏆 Badges", "👻 Ghost", "💌 Capsules")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("👤 Profile") },
            actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") } })

        // Profile header
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(ZSBlue), contentAlignment = Alignment.Center) {
                    Text(player.name.first().toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(player.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text(player.role.uppercase(), fontSize = 12.sp, color = ZSOrange)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 16.sp)
                        Text("${coinBalance?.balance ?: 0}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Coins", fontSize = 10.sp, color = Color.Gray)
                    }
                    val entry = leaderboard.find { it.playerName == player.name }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 16.sp)
                        Text("${entry?.score ?: 0}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Score", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏅", fontSize = 16.sp)
                        Text("${achievements.count { it.isUnlocked }}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Badges", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 9.sp) })
            }
        }

        when (selectedTab) {
            0 -> ProfileDetails(player, dailyLogs, leaderboard)
            1 -> TitlesTab(titles)
            2 -> BadgesTab(achievements)
            3 -> GhostModeTab()
            4 -> CapsulesTab(capsules)
        }
    }
}

@Composable
private fun ProfileDetails(player: Player, logs: List<MatchRecord>, leaderboard: List<LeaderboardEntry>) {
    val entry = leaderboard.find { it.playerName == player.name }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Lifetime Stats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Matches", "${entry?.matches ?: 0}", "🎮")
                        BigStat("Wins", "${entry?.wins ?: 0}", "🏆")
                        BigStat("Win Rate", "${"%.1f".format(entry?.winRate ?: 0.0)}%", "📈")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Kills", "${entry?.kills ?: 0}", "💀")
                        BigStat("Damage", "${entry?.damage ?: 0}", "💥")
                        BigStat("Score", "${entry?.score ?: 0}", "⭐")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Assists", "${entry?.assists ?: 0}", "🤝")
                        BigStat("Avg Kills", "${"%.1f".format(entry?.avgKills ?: 0.0)}", "🎯")
                        BigStat("Avg Damage", "${entry?.avgDamage ?: 0}", "📊")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Phone", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(player.phone, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2D1B))) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text("🎮", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Play Style", fontWeight = FontWeight.Bold, color = ZSGreen)
                        val style = when {
                            (entry?.avgKills ?: 0.0) > 5.0 -> "🔥 Aggressive Fragger"
                            (entry?.winRate ?: 0.0) > 50 -> "🧠 Tactical Player"
                            (entry?.avgDamage ?: 0) > 500 -> "💥 Damage Dealer"
                            else -> "📊 Balanced Player"
                        }
                        Text(style, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TitlesTab(titles: List<PlayerTitle>) {
    val allTitles = if (titles.isEmpty()) listOf(
        PlayerTitle(name = "Rookie", icon = "🌱", requirement = "Join the team"),
        PlayerTitle(name = "Veteran", icon = "🎖️", requirement = "Play 50 matches"),
        PlayerTitle(name = "Kill Machine", icon = "💀", requirement = "Get 100 kills"),
        PlayerTitle(name = "Clutch King", icon = "👑", requirement = "Win 10 matches in a row"),
        PlayerTitle(name = "Tank", icon = "🛡️", requirement = "Survive 600+ seconds avg"),
        PlayerTitle(name = "MVP", icon = "⭐", requirement = "Highest score on team"),
        PlayerTitle(name = "Legend", icon = "🏆", requirement = "Reach 5000 score"),
        PlayerTitle(name = "Untouchable", icon = "👻", requirement = "Win 10 matches with 0 deaths"),
    ) else titles

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("🏷️ Available Titles", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(allTitles) { title ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title.name, fontWeight = FontWeight.Bold)
                        Text(title.requirement, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (title.isUnlocked) Text("✅", fontSize = 16.sp) else Text("🔒", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun BadgesTab(achievements: List<Achievement>) {
    val allBadges = if (achievements.isEmpty()) listOf(
        Achievement(title = "First Blood", description = "Get your first kill", icon = "🩸", rarity = "common", isUnlocked = true),
        Achievement(title = "Double Kill", description = "Get 10 kills in one match", icon = "🔫", rarity = "rare"),
        Achievement(title = "Squad Leader", description = "Lead a squad to victory", icon = "👑", rarity = "epic"),
        Achievement(title = "Untouchable", description = "Win without dying", icon = "👻", rarity = "legendary"),
        Achievement(title = "Streak Master", description = "Win 5 matches in a row", icon = "🔥", rarity = "rare", isUnlocked = true),
        Achievement(title = "Damage King", description = "Deal 1000+ damage in one match", icon = "💥", rarity = "epic"),
        Achievement(title = "Survivor", description = "Survive 600+ seconds", icon = "⏱️", rarity = "common"),
        Achievement(title = "Social Butterfly", description = "Send 100 chat messages", icon = "🦋", rarity = "common"),
        Achievement(title = "Collector", description = "Own 10 shop items", icon = "🛒", rarity = "rare"),
        Achievement(title = "Night Owl", description = "Play 10 matches after midnight", icon = "🦉", rarity = "epic"),
    ) else achievements

    val rarityColors = mapOf("common" to Color.Gray, "rare" to ZSBlue, "epic" to Color(0xFF9C27B0), "legendary" to ZSOrange)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("🏆 Achievement Badges", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(allBadges) { badge ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        .background(rarityColors[badge.rarity]?.copy(alpha = 0.2f) ?: Color.Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        Text(badge.icon, fontSize = 24.sp, modifier = if (!badge.isUnlocked) Modifier else Modifier)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(badge.title, fontWeight = FontWeight.Bold)
                        Text(badge.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(badge.rarity.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = rarityColors[badge.rarity] ?: Color.Gray)
                        if (badge.isUnlocked) Text("✅ Unlocked") else Text("🔒")
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostModeTab() {
    var ghostEnabled by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👻", fontSize = 48.sp)
                    Text("Ghost Mode", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Play anonymously — your stats are hidden from others", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Ghost Mode", modifier = Modifier.weight(1f))
                        Switch(checked = ghostEnabled, onCheckedChange = { ghostEnabled = it })
                    }
                    if (ghostEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B2D)), shape = RoundedCornerShape(8.dp)) {
                            Text("👻 You are now invisible on leaderboards and profiles",
                                modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("What Ghost Mode does:", fontWeight = FontWeight.Bold)
                    Text("• Hide from public leaderboards", fontSize = 13.sp)
                    Text("• Name shows as ??? to other players", fontSize = 13.sp)
                    Text("• Stats are private", fontSize = 13.sp)
                    Text("• You can still participate in matches", fontSize = 13.sp)
                    Text("• Toggle off anytime", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CapsulesTab(capsules: List<TimeCapsule>) {
    val allCapsules = if (capsules.isEmpty()) listOf(
        TimeCapsule(authorName = "System", message = "Create a time capsule! Write a message to your future self.",
            createdAt = System.currentTimeMillis(), unlockAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, isUnlocked = false)
    ) else capsules

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💌", fontSize = 48.sp)
                    Text("Time Capsules", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Write a message to your future self", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(allCapsules) { capsule ->
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (capsule.isUnlocked) Color(0xFF1B2D1B) else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (capsule.isUnlocked) "💌" else "🔒", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (capsule.isUnlocked) "Unlocked!" else "Sealed", fontWeight = FontWeight.Bold,
                                color = if (capsule.isUnlocked) ZSGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (capsule.isUnlocked) {
                                Text(capsule.message, fontSize = 13.sp)
                            } else {
                                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                Text("Opens: ${sdf.format(Date(capsule.unlockAt))}", fontSize = 12.sp)
                                val daysLeft = ((capsule.unlockAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                                Text("${daysLeft}d remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
