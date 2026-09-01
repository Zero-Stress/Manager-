package com.zerostress.manager.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.DAILY_REWARD_TABLE
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun DailyRewardsScreen(phone: String, appViewModel: AppViewModel) {
    val rank by appViewModel.playerRank.collectAsState()
    LaunchedEffect(Unit) { appViewModel.loadRank(phone) }

    val todayCollected = rank?.lastLoginDate == java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val currentStreak = rank?.loginStreak ?: 0
    val todayRewardDay = ((currentStreak) % 7) + 1

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Daily Rewards", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Text("Login every day to earn coins!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Coin balance
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💰", fontSize = 40.sp)
                Text("${rank?.coins ?: 0}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Warning)
                Text("Coins Balance", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Login Streak: ${currentStreak} days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 7-day reward grid
        Text("This Week's Rewards", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DAILY_REWARD_TABLE.forEach { reward ->
                val isCollected = todayCollected && reward.day <= currentStreak
                val isToday = reward.day == todayRewardDay && !todayCollected

                Card(
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCollected -> Success.copy(alpha = 0.15f)
                            isToday -> Accent.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isToday) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Day ${reward.day}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("💰${reward.coins}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (isCollected) Success else if (isToday) Accent else MaterialTheme.colorScheme.onSurface)
                        if (isCollected) Text("✓", fontSize = 10.sp, color = Success)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Collect button
        Button(
            onClick = { appViewModel.collectDailyReward(phone) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (todayCollected) MaterialTheme.colorScheme.outline else Success
            ),
            enabled = !todayCollected
        ) {
            Icon(Icons.Filled.CardGiftcard, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (todayCollected) "Already Collected Today!" else "Collect Day $todayRewardDay Reward",
                fontWeight = FontWeight.ExtraBold, color = if (todayCollected) MaterialTheme.colorScheme.onSurfaceVariant else androidx.compose.ui.graphics.Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Coin store concept
        Text("Coin Shop", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "Custom Profile Theme" to 200, "Premium Title" to 150, "Special Badge" to 100,
            "Double XP Boost (1hr)" to 300, "Custom Chat Color" to 250
        ).forEach { (item, cost) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val canAfford = (rank?.coins ?: 0) >= cost
                    Button(
                        onClick = { if (canAfford) appViewModel.addCoins(phone, -cost) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) Accent else MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("💰 $cost", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (canAfford) DarkBg else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
