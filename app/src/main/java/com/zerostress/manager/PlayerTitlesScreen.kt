package com.zerostress.manager

import androidx.compose.foundation.clickable
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

private data class TitleDef(val id: String, val title: String, val emoji: String, val requirement: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTitlesScreen() {
    val uid = FirebaseAuth.getInstance().uid
    var currentTitle by remember { mutableStateOf("") }

    val titles = listOf(
        TitleDef("iron", "Iron Warrior", "⚔️", "Default"),
        TitleDef("bronze", "Bronze Fighter", "🛡️", "Play 10 matches"),
        TitleDef("silver", "Silver Slayer", "🎯", "Win 25 matches"),
        TitleDef("gold", "Gold Gladiator", "👑", "Reach Level 10"),
        TitleDef("diamond", "Diamond Destroyer", "💎", "Get 500 kills"),
        TitleDef("master", "Master Legend", "🌟", "Reach Level 25"),
        TitleDef("champion", "Grand Champion", "🏆", "Win 100 matches"),
        TitleDef("mythic", "Mythic God", "🔥", "Reach Level 50"),
    )

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get().addOnSuccessListener { doc ->
            currentTitle = doc.getString("title") ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🏷️ Player Titles", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        if (currentTitle.isNotBlank()) {
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSPrimary.copy(alpha = 0.2f))) {
                Text("Current: $currentTitle", modifier = Modifier.padding(14.dp), color = ZSPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(titles) { t ->
                Card(
                    modifier = Modifier.clickable {
                        if (uid != null) {
                            FirebaseFirestore.getInstance().collection("players").document(uid).update("title", t.title)
                            currentTitle = t.title
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (currentTitle == t.title) ZSPrimary.copy(alpha = 0.2f) else ZSCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t.emoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Text(t.requirement, color = ZSTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
