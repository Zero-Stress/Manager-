package com.zerostress.manager.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.PlayerRank
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun RankLevelScreen(phone: String, name: String, appViewModel: AppViewModel) {
    val rank by appViewModel.playerRank.collectAsState()

    LaunchedEffect(Unit) { appViewModel.loadRank(phone) }

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Rank & Level", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(16.dp))

        // Level card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LEVEL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                Text("${rank?.level ?: 1}", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                LinearProgressIndicator(
                    progress = { rank?.xpProgress() ?: 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Accent, trackColor = MaterialTheme.colorScheme.outline
                )
                Text("XP: ${rank?.xp ?: 0} / ${rank?.xpForNextLevel() ?: 150}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rank tier
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Rank Tier", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rank?.rankTier ?: "Bronze", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Gold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Coins", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("💰 ${rank?.coins ?: 0}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Warning)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current title
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.08f))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current Title", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rank?.title?.ifEmpty { "Rookie" } ?: "Rookie", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Accent)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats overview
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Win Streak", "${rank?.winStreak ?: 0}", Warning, Modifier.weight(1f))
            StatCard("Best Streak", "${rank?.bestWinStreak ?: 0}", Gold, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Login Days", "${rank?.totalLoginDays ?: 0}", Success, Modifier.weight(1f))
            StatCard("Login Streak", "${rank?.loginStreak ?: 0}d", Accent, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tier progression
        Text("Rank Tiers", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))
        PlayerRank.TIERS.forEach { (tierName, minLevel) ->
            val isCurrent = rank?.rankTier == tierName
            val isPassed = (rank?.level ?: 0) >= minLevel
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (isCurrent) Accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tierName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = if (isPassed) Accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lv.$minLevel+", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Titles
        Text("Titles", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))
        PlayerRank.TITLES.forEach { (titleName, minWins) ->
            val isCurrent = rank?.title == titleName
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (isCurrent) Accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(titleName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = if (isCurrent) Accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$minWins wins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
