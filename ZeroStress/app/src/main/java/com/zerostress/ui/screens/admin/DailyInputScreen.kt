package com.zerostress.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.MatchRecord
import com.zerostress.data.model.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyInputScreen(
    players: List<Player>,
    onSave: (MatchRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmedPlayers = players.filter { it.status == "confirmed" }
    var selectedPlayer by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf("1") }
    var wins by remember { mutableStateOf("0") }
    var kills by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("0") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("📊 Daily Input", fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp))

        // Player selector
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedPlayer, onValueChange = {}, readOnly = true,
                label = { Text("Select Player") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                confirmedPlayers.forEach { player ->
                    DropdownMenuItem(
                        text = { Text(player.name) },
                        onClick = { selectedPlayer = player.name; expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Match data inputs
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = matches, onValueChange = { matches = it },
                label = { Text("Matches") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = wins, onValueChange = { wins = it },
                label = { Text("Wins") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = kills, onValueChange = { kills = it },
                label = { Text("Kills") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = assists, onValueChange = { assists = it },
                label = { Text("Assists") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = damage, onValueChange = { damage = it },
            label = { Text("Total Damage") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = minutes, onValueChange = { minutes = it },
                label = { Text("Surv. Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = seconds, onValueChange = { seconds = it },
                label = { Text("Surv. Sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val totalMatches = matches.toIntOrNull() ?: 1
                val totalWins = wins.toIntOrNull() ?: 0
                val totalKills = kills.toIntOrNull() ?: 0
                val totalAssists = assists.toIntOrNull() ?: 0
                val totalDamage = damage.toIntOrNull() ?: 0
                val survivalSeconds = ((minutes.toIntOrNull() ?: 0) * 60) + (seconds.toIntOrNull() ?: 0)
                val avgDmg = if (totalMatches > 0) totalDamage / totalMatches else 0
                onSave(MatchRecord(
                    playerName = selectedPlayer, matches = totalMatches, wins = totalWins,
                    kills = totalKills, assists = totalAssists, damage = totalDamage,
                    avgDamage = avgDmg, survivalSeconds = survivalSeconds
                ))
                kills = ""; assists = ""; damage = ""
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = selectedPlayer.isNotEmpty() && kills.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE RECORD", fontWeight = FontWeight.Bold)
        }
    }
}
