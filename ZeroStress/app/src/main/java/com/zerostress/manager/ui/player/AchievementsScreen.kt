package com.zerostress.manager.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.ALL_ACHIEVEMENTS
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun AchievementsScreen(phone: String, appViewModel: AppViewModel) {
    val playerAchievements by appViewModel.playerAchievements.collectAsState()
    val dailyLogs by appViewModel.dailyLogs.collectAsState()

    val playerStats = dailyLogs.filter { it.phone == phone }
    val totalKills = playerStats.sumOf { it.kills }
    val totalWins = playerStats.sumOf { it.wins }
    val totalMatches = playerStats.sumOf { it.matches }
    val totalDamage = playerStats.sumOf { it.damage }

    val unlockedIds = playerAchievements.map { it.achievementId }.toSet()
    val totalAchievements = ALL_ACHIEVEMENTS.size
    val unlockedCount = ALL_ACHIEVEMENTS.count { it.id in unlockedIds }
    val progressPct = if (totalAchievements > 0) (unlockedCount.toFloat() / totalAchievements) * 100 else 0f

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Achievements", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        // Progress overview
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$unlockedCount / $totalAchievements", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                Text("Achievements Unlocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { unlockedCount.toFloat() / totalAchievements },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Accent,
                    trackColor = MaterialTheme.colorScheme.outline
                )
                Text("${String.format("%.0f", progressPct)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories
        val categories = listOf("combat" to "Combat", "milestone" to "Milestones", "consistency" to "Consistency", "social" to "Social")

        categories.forEach { (catKey, catName) ->
            val catAchievements = ALL_ACHIEVEMENTS.filter { it.category == catKey }
            if (catAchievements.isNotEmpty()) {
                Text(catName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Accent, modifier = Modifier.padding(vertical = 8.dp))
                catAchievements.forEach { achievement ->
                    val isUnlocked = achievement.id in unlockedIds
                    val currentProgress = when (achievement.type) {
                        "kills" -> totalKills
                        "wins" -> totalWins
                        "matches" -> totalMatches
                        "damage" -> totalDamage
                        else -> 0
                    }
                    val progress = (currentProgress.toDouble() / achievement.requirement).coerceIn(0.0, 1.0)

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) Accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUnlocked) Accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(achievement.icon, fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(achievement.name, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = if (isUnlocked) Accent else MaterialTheme.colorScheme.onSurface)
                                Text(achievement.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = if (isUnlocked) Success else Accent,
                                    trackColor = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "$currentProgress / ${achievement.requirement}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isUnlocked) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Success.copy(alpha = 0.15f)) {
                                    Text("✓ Done", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = Success, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
