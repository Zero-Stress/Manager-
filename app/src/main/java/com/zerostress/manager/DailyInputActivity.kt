package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

class DailyInputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { DailyInputScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyInputScreen() {
    var playerNames by remember { mutableStateOf(listOf<String>()) }
    var playerIds by remember { mutableStateOf(listOf<String>()) }
    var selectedPlayer by remember { mutableIntStateOf(0) }
    var matchType by remember { mutableIntStateOf(0) }
    var kills by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }
    var wins by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val matchTypes = listOf("Classic", "Ranked", "Tournament", "Custom")

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("players").get().addOnSuccessListener { s ->
            playerIds = s.documents.map { it.id }
            playerNames = s.documents.map { it.getString("name") ?: "Unknown" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📝 Daily Input", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Select Player", color = ZSTextMuted, fontSize = 12.sp)
                if (playerNames.isNotEmpty()) {
                    DropdownMenuBox(playerNames, selectedPlayer) { selectedPlayer = it }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Match Type", color = ZSTextMuted, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchTypes.forEachIndexed { index, type ->
                        FilterChip(selected = matchType == index, onClick = { matchType = index }, label = { Text(type) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = kills, onValueChange = { kills = it }, label = { Text("Kills") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = assists, onValueChange = { assists = it }, label = { Text("Assists") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = damage, onValueChange = { damage = it }, label = { Text("Damage") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = wins, onValueChange = { wins = it }, label = { Text("Wins (1/0)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                if (status.isNotEmpty()) {
                    Text(status, color = ZSPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (selectedPlayer < playerIds.size) {
                            val pid = playerIds[selectedPlayer]
                            val k = kills.toLongOrNull() ?: 0
                            val a = assists.toLongOrNull() ?: 0
                            val d = damage.toLongOrNull() ?: 0
                            val w = wins.toIntOrNull() ?: 0
                            val score = k * 10 + a * 5 + (d / 100) + w * 25

                            val updates = hashMapOf<String, Any>(
                                "kills" to com.google.firebase.firestore.FieldValue.increment(k),
                                "assists" to com.google.firebase.firestore.FieldValue.increment(a),
                                "damage" to com.google.firebase.firestore.FieldValue.increment(d),
                                "matches" to com.google.firebase.firestore.FieldValue.increment(1),
                                "score" to com.google.firebase.firestore.FieldValue.increment(score.toLong()),
                                "xp" to com.google.firebase.firestore.FieldValue.increment(score.toLong())
                            )
                            if (w > 0) updates["wins"] = com.google.firebase.firestore.FieldValue.increment(1)

                            FirebaseFirestore.getInstance().collection("players").document(pid).update(updates)
                                .addOnSuccessListener { status = "✅ Updated ${playerNames[selectedPlayer]} (+${score} score)" }
                                .addOnFailureListener { status = "❌ Error: ${it.message}" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit Daily Stats") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = items.getOrElse(selectedIndex) { "Select" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}
