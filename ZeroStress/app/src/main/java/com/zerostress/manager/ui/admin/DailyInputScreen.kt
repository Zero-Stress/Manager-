package com.zerostress.manager.ui.admin

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
import com.zerostress.manager.models.DailyLog
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyInputScreen(appViewModel: AppViewModel) {
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    val users by appViewModel.users.collectAsState()
    val confirmedPlayers = users.filter { it.status == "confirmed" }
    var selectedPlayer by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf("1") }
    var wins by remember { mutableStateOf("0") }
    var kills by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("0") }
    var showOcr by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Daily Match Input", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        // OCR Scanner button
        OutlinedButton(
            onClick = { showOcr = !showOcr },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(Icons.Filled.CameraAlt, null, tint = Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Auto-Fill from Screenshot (OCR)", color = Accent, fontWeight = FontWeight.Bold)
        }
        if (showOcr) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷 OCR Scanner", fontWeight = FontWeight.Bold, color = Accent)
                    Text("Crop across Kills, Assists, Damage, Revival & Survival Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("(CameraX + ML Kit integration - build in AndroidIDE)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Player selector
        Text("Select Player", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = confirmedPlayers.find { it.phone == selectedPlayer }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                confirmedPlayers.forEach { player ->
                    DropdownMenuItem(
                        text = { Text(player.name) },
                        onClick = { selectedPlayer = player.phone; expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats input grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = matches, onValueChange = { matches = it }, label = { Text("Matches") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
            OutlinedTextField(value = wins, onValueChange = { wins = it }, label = { Text("Wins") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = kills, onValueChange = { kills = it }, label = { Text("Kills") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
            OutlinedTextField(value = assists, onValueChange = { assists = it }, label = { Text("Assists") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = damage, onValueChange = { damage = it }, label = { Text("Total Damage") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
        Spacer(modifier = Modifier.height(8.dp))

        Text("Survival Time", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
            OutlinedTextField(value = seconds, onValueChange = { seconds = it }, label = { Text("Seconds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val playerName = confirmedPlayers.find { it.phone == selectedPlayer }?.name ?: return@Button
                val log = DailyLog(
                    playerName = playerName,
                    phone = selectedPlayer,
                    matches = matches.toIntOrNull() ?: 1,
                    wins = wins.toIntOrNull() ?: 0,
                    kills = kills.toIntOrNull() ?: 0,
                    assists = assists.toIntOrNull() ?: 0,
                    damage = damage.toIntOrNull() ?: 0,
                    survivalMinutes = minutes.toIntOrNull() ?: 0,
                    survivalSeconds = seconds.toIntOrNull() ?: 0,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                appViewModel.addDailyLog(log)
                kills = ""; assists = ""; damage = ""; wins = "0"
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Daily Entry", fontWeight = FontWeight.ExtraBold, color = DarkBg)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Recent Daily Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
        Spacer(modifier = Modifier.height(8.dp))

        dailyLogs.take(10).forEach { log ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(log.playerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${log.kills}K ${log.assists}A ${log.damage}DMG", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${log.score.toInt()} pts", fontWeight = FontWeight.Bold, color = Accent)
                        IconButton(onClick = { appViewModel.deleteDailyLog(log.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, null, tint = Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
