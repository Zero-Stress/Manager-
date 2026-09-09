package com.zerostress.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

private data class RewardDay(val day: Int, val reward: String, val amount: String, val emoji: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLoginRewardsScreen() {
    val uid = FirebaseAuth.getInstance().uid
    var streak by remember { mutableIntStateOf(0) }
    var claimedToday by remember { mutableStateOf(false) }

    val rewards = listOf(
        RewardDay(1, "Coins", "50", "🪙"), RewardDay(2, "XP", "100", "⭐"),
        RewardDay(3, "Coins", "100", "🪙"), RewardDay(4, "XP", "200", "⭐"),
        RewardDay(5, "Coins", "200", "💰"), RewardDay(6, "XP", "300", "⭐"),
        RewardDay(7, "Coins + Title", "500", "👑"),
    )

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                streak = (doc.getLong("loginStreak") ?: 0).toInt()
                claimedToday = doc.getBoolean("claimedToday") ?: false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🎁 Daily Login Rewards", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSPrimary.copy(alpha = 0.2f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🔥 Streak: $streak days", color = ZSPrimary, fontWeight = FontWeight.Bold)
                if (claimedToday) Text("✅ Claimed", color = ZSPrimary) else Text("Unclaimed", color = ZSTextMuted)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rewards) { r ->
                val isUnlocked = streak >= r.day
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) ZSPrimary.copy(alpha = 0.15f) else ZSCard)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(r.emoji, fontSize = 24.sp)
                        Text("Day ${r.day}", color = ZSTextMuted, fontSize = 10.sp)
                        Text(r.amount, color = if (isUnlocked) ZSPrimary else ZSTextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(r.reward, color = ZSTextMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (!claimedToday) {
            Button(
                onClick = {
                    if (uid != null) {
                        val newStreak = streak + 1
                        val rewardsMap = hashMapOf<String, Any>(
                            "loginStreak" to newStreak,
                            "claimedToday" to true,
                            "coins" to com.google.firebase.firestore.FieldValue.increment(50)
                        )
                        FirebaseFirestore.getInstance().collection("players").document(uid).update(rewardsMap)
                        streak = newStreak
                        claimedToday = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎁 Claim Today's Reward") }
        }
    }
}
