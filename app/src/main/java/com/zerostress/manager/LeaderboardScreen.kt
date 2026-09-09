package com.zerostress.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen() {
    val tabs = listOf("Daily", "Weekly", "Monthly")
    var selectedTab by remember { mutableIntStateOf(0) }
    var players by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    val uid = FirebaseAuth.getInstance().uid

    LaunchedEffect(selectedTab) {
        FirebaseFirestore.getInstance().collection("players").get()
            .addOnSuccessListener { result ->
                val sorted = result.documents.sortedByDescending { it.getLong("score") ?: 0 }
                players = sorted
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🏆 Leaderboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(players) { index, doc ->
                val name = doc.getString("name") ?: "Unknown"
                val score = doc.getLong("score") ?: 0
                val kills = doc.getLong("kills") ?: 0
                val isMe = doc.id == uid
                val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}" }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isMe) ZSPrimary.copy(alpha = 0.2f) else ZSCard)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(medal, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                            Column {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("$kills kills", color = ZSTextMuted, fontSize = 12.sp)
                            }
                        }
                        Text("$score", color = ZSPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
