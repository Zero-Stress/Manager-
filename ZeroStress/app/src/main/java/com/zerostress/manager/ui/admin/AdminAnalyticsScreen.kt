package com.zerostress.manager.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.AdminSettings
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@Composable
fun AdminAnalyticsScreen(phone: String, appViewModel: AppViewModel) {
    val users by appViewModel.users.collectAsState()
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    val announcements by appViewModel.announcements.collectAsState()
    val schedules by appViewModel.schedules.collectAsState()
    val allRanks by appViewModel.allRanks.collectAsState()
    val settings by appViewModel.adminSettings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { appViewModel.loadAdminSettings() }

    val totalPlayers = users.size
    val confirmedPlayers = users.count { it.status == "confirmed" }
    val pendingPlayers = users.count { it.status == "pending" }
    val totalMatches = dailyLogs.sumOf { it.matches }
    val totalKills = dailyLogs.sumOf { it.kills }
    val totalDamage = dailyLogs.sumOf { it.damage }

    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Admin Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Filled.Settings, "Settings", tint = Accent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Stats grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsCard("Total Players", "$totalPlayers", Accent, Modifier.weight(1f))
            AnalyticsCard("Active", "$confirmedPlayers", Success, Modifier.weight(1f))
            AnalyticsCard("Pending", "$pendingPlayers", Warning, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsCard("Total Matches", "$totalMatches", Gold, Modifier.weight(1f))
            AnalyticsCard("Total Kills", "$totalKills", Danger, Modifier.weight(1f))
            AnalyticsCard("Total Damage", "${totalDamage / 1000}K", Accent, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity metrics
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Activity Metrics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow("Announcements", "${announcements.size}", Icons.Filled.Campaign)
                MetricRow("Match Schedules", "${schedules.size}", Icons.Filled.CalendarMonth)
                MetricRow("Registered Players", "$totalPlayers", Icons.Filled.People)
                MetricRow("Avg Score/Player", if (confirmedPlayers > 0) "${dailyLogs.sumOf { it.score.toInt() } / confirmedPlayers}" else "0", Icons.Filled.TrendingUp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top players
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Top Players by Level", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Accent)
                Spacer(modifier = Modifier.height(8.dp))
                allRanks.sortedByDescending { it.level }.take(5).forEachIndexed { index, rank ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${rank.phone}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Lv.${rank.level} | ${rank.rankTier}", fontSize = 13.sp, color = Accent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Banned players
        if (settings.bannedPlayers.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.05f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Banned Players", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Danger)
                    Spacer(modifier = Modifier.height(8.dp))
                    settings.bannedPlayers.forEach { bPhone ->
                        val bName = users.find { it.phone == bPhone }?.name ?: bPhone
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("$bName (+880 $bPhone)", fontSize = 13.sp)
                            IconButton(onClick = { appViewModel.unbanPlayer(bPhone) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.RemoveCircle, "Unban", tint = Success, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        var formula by remember { mutableStateOf(settings.customScoreFormula) }
        var whitelist by remember { mutableStateOf(settings.whitelistMode) }
        var banPhone by remember { mutableStateOf("") }

        AlertDialog(onDismissRequest = { showSettingsDialog = false },
            title = { Text("Admin Settings", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                Column {
                    Text("Custom Score Formula", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(value = formula, onValueChange = { formula = it }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Whitelist Mode", modifier = Modifier.weight(1f))
                        Switch(checked = whitelist, onCheckedChange = { whitelist = it })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ban Player (phone)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(value = banPhone, onValueChange = { banPhone = it }, placeholder = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = {
                        if (banPhone.isNotBlank()) { appViewModel.banPlayer(banPhone); banPhone = "" }
                    }) { Text("Ban", color = Danger) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    appViewModel.updateAdminSettings(settings.copy(customScoreFormula = formula, whitelistMode = whitelist))
                    showSettingsDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AnalyticsCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
