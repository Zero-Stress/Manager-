package com.zerostress.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ProfileScreen() {
    val uid = FirebaseAuth.getInstance().uid
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0L) }
    var level by remember { mutableStateOf(1L) }
    var rank by remember { mutableStateOf("Iron") }
    var kills by remember { mutableStateOf(0L) }
    var deaths by remember { mutableStateOf(0L) }
    var assists by remember { mutableStateOf(0L) }
    var damage by remember { mutableStateOf(0L) }
    var wins by remember { mutableStateOf(0L) }
    var matches by remember { mutableStateOf(0L) }
    var coins by remember { mutableStateOf(0L) }
    var xp by remember { mutableStateOf(0L) }

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                    score = doc.getLong("score") ?: 0
                    level = doc.getLong("level") ?: 1
                    rank = doc.getString("rank") ?: "Iron"
                    kills = doc.getLong("kills") ?: 0
                    deaths = doc.getLong("deaths") ?: 0
                    assists = doc.getLong("assists") ?: 0
                    damage = doc.getLong("damage") ?: 0
                    wins = doc.getLong("wins") ?: 0
                    matches = doc.getLong("matches") ?: 0
                    coins = doc.getLong("coins") ?: 0
                    xp = doc.getLong("xp") ?: 0
                    editName = name
                }
            }
    }

    val winRate = if (matches > 0) (wins * 100 / matches).toInt() else 0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("👤 Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Card(modifier = Modifier.size(80.dp), shape = CircleShape, colors = CardDefaults.cardColors(containerColor = ZSPrimary.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 36.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isEditing) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (editName.isNotBlank() && uid != null) {
                                FirebaseFirestore.getInstance().collection("players").document(uid)
                                    .update("name", editName.trim())
                                name = editName.trim()
                                isEditing = false
                            }
                        }) { Text("Save") }
                        OutlinedButton(onClick = { isEditing = false; editName = name }) { Text("Cancel") }
                    }
                } else {
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(phone, fontSize = 13.sp, color = ZSTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { isEditing = true }) { Text("Edit Profile") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Stats", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileStatItem("Score", "$score")
                    ProfileStatItem("Level", "$level")
                    ProfileStatItem("Rank", rank)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileStatItem("Kills", "$kills")
                    ProfileStatItem("Deaths", "$deaths")
                    ProfileStatItem("Assists", "$assists")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileStatItem("Damage", "$damage")
                    ProfileStatItem("Wins", "$wins")
                    ProfileStatItem("Matches", "$matches")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileStatItem("Win Rate", "${winRate}%")
                    ProfileStatItem("Coins", "$coins")
                    ProfileStatItem("XP", "$xp")
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(modifier = Modifier.padding(4.dp)) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZSPrimary)
        Text(label, fontSize = 11.sp, color = ZSTextMuted)
    }
}
