package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class AchievementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { AchievementsScreen() } }
    }
}

private data class AchievementDef(val id: String, val title: String, val desc: String, val xp: String, val coins: String, val emoji: String)

@Composable
fun AchievementsScreen() {
    val userId = FirebaseAuth.getInstance().uid
    var unlockedIds by remember { mutableStateOf(listOf<String>()) }

    val achievements = listOf(
        AchievementDef("first_blood", "First Blood", "Get your first kill", "10", "50", "🔫"),
        AchievementDef("kill_100", "Century Killer", "Get 100 total kills", "50", "200", "💀"),
        AchievementDef("kill_500", "Rampage", "Get 500 total kills", "100", "500", "🔥"),
        AchievementDef("win_10", "Winner", "Win 10 matches", "50", "250", "🏆"),
        AchievementDef("win_50", "Champion", "Win 50 matches", "200", "1000", "👑"),
        AchievementDef("damage_10000", "Damage Dealer", "Deal 10,000 total damage", "100", "300", "💥"),
        AchievementDef("level_5", "Rising Star", "Reach Level 5", "50", "150", "⭐"),
        AchievementDef("level_10", "Veteran", "Reach Level 10", "150", "500", "🎖️"),
        AchievementDef("level_25", "Legend", "Reach Level 25", "500", "2000", "🏅"),
    )

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(userId).get()
            .addOnSuccessListener { doc ->
                val raw = doc.get("unlockedAchievements")
                if (raw is List<*>) unlockedIds = raw.mapNotNull { it as? String }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🏅 Achievements", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(achievements) { ach ->
                val unlocked = ach.id in unlockedIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (unlocked) ZSPrimary.copy(alpha = 0.15f) else ZSCard)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(ach.emoji, fontSize = 28.sp, modifier = Modifier.width(40.dp))
                            Column {
                                Text(ach.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(ach.desc, color = ZSTextMuted, fontSize = 12.sp)
                            }
                        }
                        if (unlocked) Text("✅", fontSize = 20.sp) else Text("+${ach.xp}XP", color = ZSPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}