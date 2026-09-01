package com.zerostress.manager.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.zerostress.manager.models.MatchSchedule
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScheduleScreen(role: String, appViewModel: AppViewModel) {
    val schedules by appViewModel.schedules.collectAsState()
    val isAdmin = role == "admin"
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Match Schedule", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
            if (isAdmin) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Match", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBg)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (schedules.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No matches scheduled yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn {
            items(schedules) { schedule ->
                val statusColor = when (schedule.status) {
                    "upcoming" -> Accent
                    "live" -> Danger
                    "completed" -> Success
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                                Text(schedule.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                    color = statusColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                        }
                        if (schedule.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(schedule.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("📅 ${schedule.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("⏰ ${schedule.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (schedule.map.isNotBlank() || schedule.mode.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (schedule.map.isNotBlank()) Text("🗺️ ${schedule.map}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (schedule.mode.isNotBlank()) Text("🎯 ${schedule.mode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (schedule.status == "upcoming") {
                                    TextButton(onClick = { appViewModel.updateScheduleStatus(schedule.id, "live") }) {
                                        Text("Start", color = Danger, fontSize = 12.sp)
                                    }
                                }
                                if (schedule.status == "live") {
                                    TextButton(onClick = { appViewModel.updateScheduleStatus(schedule.id, "completed") }) {
                                        Text("Complete", color = Success, fontSize = 12.sp)
                                    }
                                }
                                TextButton(onClick = { appViewModel.deleteSchedule(schedule.id) }) {
                                    Text("Delete", color = Danger, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var map by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Schedule Match", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 2026-09-05)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (e.g. 20:00)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = map, onValueChange = { map = it }, label = { Text("Map (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mode, onValueChange = { mode = it }, label = { Text("Mode (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && date.isNotBlank()) {
                        appViewModel.createSchedule(MatchSchedule(title = title, description = desc, date = date, time = time, map = map, mode = mode))
                        showCreateDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
