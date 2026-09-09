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
fun SeasonScreen() {
    var seasons by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var currentSeason by remember { mutableStateOf("None") }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("seasons").limit(50).addSnapshotListener { s, _ ->
            if (s != null) {
                seasons = s.documents.sortedByDescending { it.getLong("createdAt") ?: 0 }
                currentSeason = seasons.firstOrNull { it.getBoolean("isActive") == true }?.getString("name") ?: "None"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🌍 Seasons", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSPrimary.copy(alpha = 0.2f))) {
            Text("Current: $currentSeason", modifier = Modifier.padding(14.dp), color = ZSPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (seasons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No seasons available", color = ZSTextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(seasons) { doc ->
                    val name = doc.getString("name") ?: "Season"
                    val isActive = doc.getBoolean("isActive") ?: false
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(doc.getString("description") ?: "", color = ZSTextMuted, fontSize = 13.sp)
                            }
                            if (isActive) Text("🟢 Active", color = ZSPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
