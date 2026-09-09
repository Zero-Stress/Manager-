package com.zerostress.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.fcm.FCMConfig
import com.zerostress.manager.fcm.ZSFCMService
import com.zerostress.manager.ui.theme.*

@Composable
fun PlayerDashboardScreen(onNavigate: (String) -> Unit) {
    val uid = FirebaseAuth.getInstance().uid
    var playerName by remember { mutableStateOf("Player") }
    var score by remember { mutableStateOf(0L) }
    var level by remember { mutableStateOf(1L) }
    var rank by remember { mutableStateOf("Iron") }
    var coins by remember { mutableStateOf(0L) }
    var xp by remember { mutableStateOf(0L) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    playerName = doc.getString("name") ?: "Player"
                    score = doc.getLong("score") ?: 0
                    level = doc.getLong("level") ?: 1
                    rank = doc.getString("rank") ?: "Iron"
                    coins = doc.getLong("coins") ?: 0
                    xp = doc.getLong("xp") ?: 0
                }
            }
    }

    val navItems = listOf(
        Triple("Schedule", Icons.Default.CalendarMonth, Routes.SCHEDULE),
        Triple("Leaderboard", Icons.Default.EmojiEvents, Routes.LEADERBOARD),
        Triple("Chat", Icons.Default.Chat, Routes.CHAT),
        Triple("Voice", Icons.Default.RecordVoiceOver, Routes.VOICE),
        Triple("Profile", Icons.Default.Person, Routes.PROFILE),
        Triple("Friends", Icons.Default.Group, Routes.FRIENDS),
        Triple("Seasons", Icons.Default.Event, Routes.SEASONS),
        Triple("Achievements", Icons.Default.EmojiEvents, Routes.ACHIEVEMENTS),
        Triple("News", Icons.Default.Info, Routes.ANNOUNCEMENTS),
        Triple("Daily Rewards", Icons.Default.CardGiftcard, Routes.DAILY_LOGIN_REWARDS),
        Triple("Challenges", Icons.Default.EmojiEvents, Routes.DAILY_CHALLENGES),
        Triple("Battle Pass", Icons.Default.Stars, Routes.BATTLE_PASS),
        Triple("Titles", Icons.Default.Label, Routes.PLAYER_TITLES),
        Triple("Performance", Icons.Default.TrendingUp, Routes.PERFORMANCE_GRAPHS),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZSBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ZSCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("👋 Welcome, $playerName", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Score", "$score")
                    StatItem("Level", "$level")
                    StatItem("Rank", rank)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Coins", "$coins")
                    StatItem("XP", "$xp/${level * 500}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation grid
        navItems.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, icon, route) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ZSCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, contentDescription = label, tint = ZSPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, color = ZSText, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Settings + Notifications + Logout
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onNavigate(Routes.SETTINGS) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(4.dp)); Text("Settings") }
            OutlinedButton(
                onClick = { onNavigate(Routes.NOTIFICATIONS) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(4.dp)); Text("Alerts") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onNavigate(Routes.LOGIN)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZSDanger)
        ) { Text("Logout") }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ZSPrimary)
        Text(label, fontSize = 11.sp, color = ZSTextMuted)
    }
}
