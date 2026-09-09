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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

@Composable
fun ViewAllPlayersStatsScreen() {
    var players by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("players").get().addOnSuccessListener { s ->
            players = s.documents.sortedByDescending { it.getLong("score") ?: 0 }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 All Players Stats", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${players.size} players", color = ZSTextMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(players) { doc ->
                    val name = doc.getString("name") ?: "Unknown"
                    val kills = doc.getLong("kills") ?: 0
                    val deaths = doc.getLong("deaths") ?: 0
                    val wins = doc.getLong("wins") ?: 0
                    val matches = doc.getLong("matches") ?: 0
                    val score = doc.getLong("score") ?: 0
                    val rank = doc.getString("rank") ?: "Iron"
                    val kd = if (deaths > 0) kills.toFloat() / deaths else kills.toFloat()

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("$score pts", color = ZSPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text("$rank • K:$kills D:$deaths W:$wins M:$matches K/D:${"%.2f".format(kd)}", color = ZSTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
