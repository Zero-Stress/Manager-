package com.zerostress.manager.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.Season
import com.zerostress.manager.models.SeasonSnapshot
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonScreen(phone: String, role: String, appViewModel: AppViewModel) {
    val seasons by appViewModel.seasons.collectAsState()
    val snapshots by appViewModel.seasonSnapshots.collectAsState()
    val isAdmin = role == "admin"
    var selectedSeason by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeSeason = seasons.find { it.isActive }

    Column(modifier = Modifier.padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Seasons", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
            if (isAdmin) {
                Button(onClick = { showCreateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Season", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBg)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active season banner
        if (activeSeason != null) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Accent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Active Season", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(activeSeason.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📅 ${activeSeason.startDate} → ${activeSeason.endDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { selectedSeason = activeSeason.id; appViewModel.loadSeasonSnapshots(activeSeason.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Success), shape = RoundedCornerShape(8.dp)) {
                                Text("View Rankings", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { appViewModel.endSeason(activeSeason.id) }, shape = RoundedCornerShape(8.dp)) {
                                Text("End Season", color = Warning, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No active season", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Season selector
        if (seasons.isNotEmpty()) {
            Text("Past Seasons", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Accent)
            Spacer(modifier = Modifier.height(8.dp))

            seasons.filter { !it.isActive }.forEach { season ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(season.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${season.startDate} → ${season.endDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { selectedSeason = season.id; appViewModel.loadSeasonSnapshots(season.id) }) {
                            Text("View", color = Accent, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Season rankings
        if (selectedSeason.isNotEmpty() && snapshots.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Season Rankings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Accent)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                itemsIndexed(snapshots) { index, snap ->
                    val rankEmoji = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}" }
                    val bgColor = when (index) { 0 -> Rank1; 1 -> Rank2; 2 -> Rank3; else -> MaterialTheme.colorScheme.surface }

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(rankEmoji, fontSize = 22.sp, modifier = Modifier.width(36.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(snap.playerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${snap.totalMatches}M | ${snap.totalWins}W | ${snap.totalKills}K | ${snap.totalDamage}DMG", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${snap.finalScore.toInt()} pts", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Accent)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Season", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Season Name (e.g. Season 1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && startDate.isNotBlank()) {
                        appViewModel.createSeason(Season(name = name, startDate = startDate, endDate = endDate.ifBlank { "TBD" }))
                        appViewModel.setActiveSeason(name) // Will activate after creation
                        showCreateDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
