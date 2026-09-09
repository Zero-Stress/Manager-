package com.zerostress.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

private data class BattlePassTier(val level: Int, val freeReward: String, val premiumReward: String, val emoji: String)

@Composable
fun BattlePassScreen() {
    val uid = FirebaseAuth.getInstance().uid
    var currentLevel by remember { mutableIntStateOf(1) }
    var currentXP by remember { mutableIntStateOf(0) }

    val tiers = (1..20).map { i ->
        when {
            i % 5 == 0 -> BattlePassTier(i, "200 XP", "Rare Skin 🎨", "⭐")
            i % 3 == 0 -> BattlePassTier(i, "100 Coins", "500 Coins", "💰")
            else -> BattlePassTier(i, "50 XP", "100 XP", "🎁")
        }
    }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                currentLevel = (doc.getLong("level") ?: 1).toInt()
                currentXP = (doc.getLong("xp") ?: 0).toInt()
            }
        }
    }

    val xpForNext = currentLevel * 500
    val progress = if (xpForNext > 0) (currentXP.toFloat() / xpForNext).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("⭐ Battle Pass", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Level $currentLevel", color = ZSPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = ZSPrimary, trackColor = ZSPrimary.copy(alpha = 0.15f))
                Text("$currentXP / $xpForNext XP", color = ZSTextMuted, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tiers) { tier ->
                val isUnlocked = currentLevel >= tier.level
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) ZSPrimary.copy(alpha = 0.1f) else ZSCard)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tier.emoji, fontSize = 20.sp, modifier = Modifier.width(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Level ${tier.level}", color = if (isUnlocked) ZSPrimary else ZSTextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Free: ${tier.freeReward}", color = ZSText, fontSize = 12.sp)
                        }
                        Text(tier.premiumReward, color = ZSWarning, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
