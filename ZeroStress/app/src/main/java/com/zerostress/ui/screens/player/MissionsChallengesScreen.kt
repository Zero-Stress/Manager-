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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.ZSBlue
import com.zerostress.ui.theme.ZSGreen
import com.zerostress.ui.theme.ZSOrange
import com.zerostress.ui.theme.ZSRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsChallengesScreen(
    missions: List<Mission>,
    seasonPass: SeasonPass?,
    onClaimMission: (String) -> Unit,
    onClaimSeasonReward: (Int) -> Unit,
    onCreateMission: (Mission) -> Unit,
    isAdmin: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🎯 Missions", "🎫 Season Pass", "💀 Boss Fights")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("🎯 Missions & Challenges") })

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp) })
            }
        }

        when (selectedTab) {
            0 -> MissionsTab(missions, onClaimMission, isAdmin, onCreateMission)
            1 -> SeasonPassTab(seasonPass, onClaimSeasonReward)
            2 -> BossFightsTab(missions.filter { it.type == "boss" }, onClaimMission)
        }
    }
}

@Composable
private fun MissionsTab(missions: List<Mission>, onClaim: (String) -> Unit, isAdmin: Boolean, onCreate: (Mission) -> Unit) {
    val dailyMissions = missions.filter { it.type == "daily" }
    val weeklyMissions = missions.filter { it.type == "weekly" }
    val reverseMissions = missions.filter { it.type == "reverse" }
    val scavenger = missions.filter { it.type == "scavenger" }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("📋 Daily Missions", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        if (dailyMissions.isEmpty()) {
            item { EmptyMissionCard("Complete matches to unlock daily missions!") }
        }
        items(dailyMissions) { mission ->
            MissionCard(mission, onClaim)
        }

        item { Spacer(modifier = Modifier.height(8.dp)); Text("📅 Weekly Challenges", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (weeklyMissions.isEmpty()) {
            item { EmptyMissionCard("Weekly challenges appear here!") }
        }
        items(weeklyMissions) { mission ->
            MissionCard(mission, onClaim)
        }

        if (reverseMissions.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(8.dp)); Text("🔄 Reverse Challenges", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(reverseMissions) { mission -> MissionCard(mission, onClaim) }
        }

        if (scavenger.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(8.dp)); Text("🔍 Scavenger Hunt", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(scavenger) { mission -> MissionCard(mission, onClaim) }
        }
    }
}

@Composable
private fun MissionCard(mission: Mission, onClaim: (String) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when(mission.type) { "daily" -> "🎯"; "weekly" -> "📅"; "boss" -> "💀"; "reverse" -> "🔄"; "scavenger" -> "🔍"; else -> "🎯" }, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(mission.title, fontWeight = FontWeight.Bold)
                    Text(mission.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${mission.rewardCoins} 🪙", color = ZSOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("+${mission.rewardXP} XP", color = ZSBlue, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { mission.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${mission.currentValue}/${mission.targetValue}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                if (mission.isCompleted && !mission.isClaimed) {
                    Button(onClick = { onClaim(mission.id) }, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZSGreen)) {
                        Text("CLAIM", fontWeight = FontWeight.Bold)
                    }
                } else if (mission.isClaimed) {
                    Text("✅ Claimed", color = ZSGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SeasonPassTab(seasonPass: SeasonPass?, onClaimReward: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎫", fontSize = 40.sp)
                    Text(seasonPass?.seasonName ?: "Season 1", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    val level = seasonPass?.currentLevel ?: 1
                    val xp = seasonPass?.currentXP ?: 0
                    val xpNeeded = seasonPass?.xpToNextLevel ?: 100
                    Text("Level $level", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZSGreen)
                    LinearProgressIndicator(progress = { xp.toFloat() / xpNeeded },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = ZSBlue)
                    Text("$xp / $xpNeeded XP", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)); Text("🎁 Rewards", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        val rewards = seasonPass?.rewards ?: (1..20).map { SeasonReward(level = it, freeReward = "🪙 ${it * 10} Coins", premiumReward = "⭐ Special Badge") }
        items(rewards) { reward ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ZSBlue), contentAlignment = Alignment.Center) {
                        Text("${reward.level}", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Free: ${reward.freeReward}", fontSize = 13.sp)
                        Text("Premium: ${reward.premiumReward}", fontSize = 13.sp, color = ZSOrange)
                    }
                    val isReachable = (seasonPass?.currentLevel ?: 1) >= reward.level
                    if (isReachable && !reward.isClaimed) {
                        Button(onClick = { onClaimReward(reward.level) }, shape = RoundedCornerShape(8.dp)) { Text("Claim") }
                    } else if (reward.isClaimed) {
                        Text("✅", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BossFightsTab(bossMissions: List<Mission>, onClaim: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (bossMissions.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💀", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Active Boss Fights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Team vs team events will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(bossMissions) { mission ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B1B))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💀", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mission.title, fontWeight = FontWeight.Bold, color = ZSRed)
                            Text(mission.description, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { mission.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = ZSRed)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${mission.currentValue}/${mission.targetValue}", fontSize = 12.sp)
                        Text("Reward: +${mission.rewardCoins} 🪙 +${mission.rewardXP} XP", fontSize = 12.sp, color = ZSOrange)
                    }
                    if (mission.isCompleted && !mission.isClaimed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onClaim(mission.id) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ZSRed)) {
                            Text("CLAIM BOSS REWARD")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMissionCard(text: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
