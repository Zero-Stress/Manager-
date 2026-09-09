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

private data class ChallengeDef(val id: String, val title: String, val desc: String, val reward: String, val emoji: String)

@Composable
fun DailyChallengesScreen() {
    val uid = FirebaseAuth.getInstance().uid
    var completedCount by remember { mutableIntStateOf(0) }

    val challenges = listOf(
        ChallengeDef("play_3", "Play 3 Matches", "Complete 3 matches today", "100 XP", "🎮"),
        ChallengeDef("win_1", "Get a Win", "Win at least 1 match", "150 XP", "🏆"),
        ChallengeDef("kill_10", "Get 10 Kills", "Achieve 10 kills today", "200 XP", "💀"),
        ChallengeDef("deal_5k", "Deal 5000 Damage", "Deal 5000+ total damage", "150 XP", "💥"),
        ChallengeDef("assist_5", "Team Player", "Get 5 assists", "100 XP", "🤝"),
        ChallengeDef("login", "Daily Login", "Log in today", "50 XP", "✅"),
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🎯 Daily Challenges", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Complete challenges to earn XP and rewards!", color = ZSTextMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSPrimary.copy(alpha = 0.15f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", color = Color.White, fontWeight = FontWeight.Bold)
                Text("$completedCount/${challenges.size}", color = ZSPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(challenges) { c ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(c.emoji, fontSize = 24.sp, modifier = Modifier.width(36.dp))
                            Column {
                                Text(c.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(c.desc, color = ZSTextMuted, fontSize = 12.sp)
                            }
                        }
                        Text(c.reward, color = ZSPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
