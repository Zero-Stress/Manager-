package com.zerostress.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitMatchScreen() {
    var kills by remember { mutableStateOf("") }
    var deaths by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }
    var matchType by remember { mutableIntStateOf(0) }
    var result by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    val types = listOf("Classic", "Ranked", "Tournament", "Custom")
    val results = listOf("Win", "Loss", "Draw")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📝 Submit Match", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Match Type", color = ZSTextMuted, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEachIndexed { index, type ->
                        FilterChip(selected = matchType == index, onClick = { matchType = index }, label = { Text(type) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = kills, onValueChange = { kills = it }, label = { Text("Kills") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = deaths, onValueChange = { deaths = it }, label = { Text("Deaths") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = assists, onValueChange = { assists = it }, label = { Text("Assists") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = damage, onValueChange = { damage = it }, label = { Text("Damage") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Result", color = ZSTextMuted, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEachIndexed { index, r ->
                        FilterChip(selected = result == index, onClick = { result = index }, label = { Text(r) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val uid = FirebaseAuth.getInstance().uid ?: return@Button
                        isSubmitting = true
                        val matchData = mapOf(
                            "playerId" to uid,
                            "kills" to (kills.toLongOrNull() ?: 0),
                            "deaths" to (deaths.toLongOrNull() ?: 0),
                            "assists" to (assists.toLongOrNull() ?: 0),
                            "damage" to (damage.toLongOrNull() ?: 0),
                            "type" to types[matchType],
                            "result" to results[result],
                            "date" to System.currentTimeMillis()
                        )
                        FirebaseFirestore.getInstance().collection("match_logs").add(matchData)
                            .addOnSuccessListener {
                                // Update player stats
                                val updates = hashMapOf<String, Any>(
                                    "kills" to com.google.firebase.firestore.FieldValue.increment(kills.toLongOrNull() ?: 0),
                                    "deaths" to com.google.firebase.firestore.FieldValue.increment(deaths.toLongOrNull() ?: 0),
                                    "assists" to com.google.firebase.firestore.FieldValue.increment(assists.toLongOrNull() ?: 0),
                                    "damage" to com.google.firebase.firestore.FieldValue.increment(damage.toLongOrNull() ?: 0),
                                    "matches" to com.google.firebase.firestore.FieldValue.increment(1)
                                )
                                if (result == 0) updates["wins"] = com.google.firebase.firestore.FieldValue.increment(1)
                                FirebaseFirestore.getInstance().collection("players").document(uid).update(updates)
                                isSubmitting = false
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) { Text("Submit Match") }
            }
        }
    }
}
