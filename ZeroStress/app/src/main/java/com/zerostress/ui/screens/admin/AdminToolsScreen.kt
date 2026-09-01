package com.zerostress.ui.screens.admin

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
import com.zerostress.data.model.*
import com.zerostress.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminToolsScreen(
    trainingSessions: List<TrainingSession>,
    scrimmages: List<Scrimmage>,
    expenses: List<ExpenseEntry>,
    players: List<Player>,
    dailyLogs: List<MatchRecord>,
    onCreateTraining: (TrainingSession) -> Unit,
    onCreateScrimmage: (Scrimmage) -> Unit,
    onAddExpense: (ExpenseEntry) -> Unit,
    onDeleteExpense: (String) -> Unit,
    currentAdminName: String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏋️ Training", "⚔️ Scrimmage", "💰 Expenses", "📊 Reports")
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("🛠️ Admin Tools") },
            actions = {
                if (selectedTab < 3) {
                    IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "Add") }
                }
            })

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 10.sp) })
            }
        }

        when (selectedTab) {
            0 -> TrainingTab(trainingSessions, players)
            1 -> ScrimmageTab(scrimmages)
            2 -> ExpenseTab(expenses, onDeleteExpense)
            3 -> ReportsTab(players, dailyLogs)
        }
    }

    if (showCreateDialog) {
        when (selectedTab) {
            0 -> CreateTrainingDialog(currentAdminName, players, onDismiss = { showCreateDialog = false },
                onCreate = { onCreateTraining(it); showCreateDialog = false })
            1 -> CreateScrimmageDialog(currentAdminName, onDismiss = { showCreateDialog = false },
                onCreate = { onCreateScrimmage(it); showCreateDialog = false })
            2 -> AddExpenseDialog(currentAdminName, onDismiss = { showCreateDialog = false },
                onAdd = { onAddExpense(it); showCreateDialog = false })
        }
    }
}

@Composable
private fun TrainingTab(sessions: List<TrainingSession>, players: List<Player>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sessions.isEmpty()) {
            item { EmptyCard("🏋️", "No training sessions", "Schedule a training session for your team") }
        }
        items(sessions) { session ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏋️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.title, fontWeight = FontWeight.Bold)
                            Text("${formatDate(session.date)} | ${session.duration} min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (session.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(session.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val attended = session.attendees.size
                    val total = session.allPlayers.size
                    LinearProgressIndicator(progress = { if (total > 0) attended.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp), color = ZSGreen)
                    Text("Attendance: $attended/$total", fontSize = 12.sp, color = ZSGreen)
                }
            }
        }
    }
}

@Composable
private fun ScrimmageTab(scrimmages: List<Scrimmage>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (scrimmages.isEmpty()) {
            item { EmptyCard("⚔️", "No scrimmages", "Schedule a practice match with another team") }
        }
        items(scrimmages) { scrim ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("vs ${scrim.opponentTeam}", fontWeight = FontWeight.Bold)
                            Text(formatDate(scrim.scheduledTime), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(
                            containerColor = when(scrim.status) { "confirmed" -> ZSGreen; "completed" -> ZSBlue; "cancelled" -> ZSRed; else -> ZSOrange }
                        )) {
                            Text(scrim.status.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (scrim.status == "completed") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Score: ${scrim.ourScore} - ${scrim.theirScore}", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = if (scrim.ourScore > scrim.theirScore) ZSGreen else ZSRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseTab(expenses: List<ExpenseEntry>, onDelete: (String) -> Unit) {
    val totalExpenses = expenses.sumOf { it.amount }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💰 Total Expenses", color = Color.Gray, fontSize = 12.sp)
                    Text("৳$totalExpenses", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                }
            }
        }
        items(expenses) { expense ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when(expense.category) { "entry_fee" -> "🎮"; "prize" -> "🏆"; "equipment" -> "🎮"; else -> "📦" }, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.description, fontWeight = FontWeight.Bold)
                        Text("${formatDate(expense.date)} | ${expense.category.uppercase()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("৳${expense.amount}", fontWeight = FontWeight.Bold, color = ZSRed)
                    IconButton(onClick = { onDelete(expense.id) }) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(players: List<Player>, dailyLogs: List<MatchRecord>) {
    val confirmedPlayers = players.filter { it.status == "confirmed" }
    val totalMatches = dailyLogs.sumOf { it.matches }
    val totalKills = dailyLogs.sumOf { it.kills }
    val totalDamage = dailyLogs.sumOf { it.damage }
    val avgWinRate = if (totalMatches > 0) dailyLogs.sumOf { it.wins }.toDouble() / totalMatches * 100 else 0.0

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("📊 Team Report Card", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 Summary", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportRow("Total Players", "${confirmedPlayers.size}")
                    ReportRow("Total Matches", "$totalMatches")
                    ReportRow("Total Kills", "$totalKills")
                    ReportRow("Total Damage", "$totalDamage")
                    ReportRow("Avg Win Rate", "${"%.1f".format(avgWinRate)}%")
                }
            }
        }

        item {
            Text("🏆 Top Performers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        val topPlayers = dailyLogs.groupBy { it.playerName }.map { (name, logs) ->
            name to MatchRecord.calculateScore(logs.sumOf { it.kills }, logs.sumOf { it.damage }, logs.sumOf { it.wins })
        }.sortedByDescending { it.second }.take(5)

        items(topPlayers) { (name, score) ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${topPlayers.indexOf(topPlayers.find { it.first == name }) + 1}.", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("⭐ $score", color = ZSOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCard(icon: String, title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CreateTrainingDialog(admin: String, players: List<Player>, onDismiss: () -> Unit, onCreate: (TrainingSession) -> Unit) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Schedule Training") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (min)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onCreate(TrainingSession(title = title, date = System.currentTimeMillis(),
            duration = duration.toIntOrNull() ?: 60, notes = notes, allPlayers = players.map { it.name }, createdBy = admin)) },
            enabled = title.isNotBlank()) { Text("Schedule") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateScrimmageDialog(admin: String, onDismiss: () -> Unit, onCreate: (Scrimmage) -> Unit) {
    var opponent by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Schedule Scrimmage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = opponent, onValueChange = { opponent = it }, label = { Text("Opponent Team") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onCreate(Scrimmage(opponentTeam = opponent, scheduledTime = System.currentTimeMillis(), createdBy = admin)) },
            enabled = opponent.isNotBlank()) { Text("Schedule") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddExpenseDialog(admin: String, onDismiss: () -> Unit, onAdd: (ExpenseEntry) -> Unit) {
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (৳)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("entry_fee" to "🎮 Fee", "prize" to "🏆 Prize", "equipment" to "💻 Gear", "other" to "📦 Other").forEach { (key, label) ->
                        FilterChip(selected = category == key, onClick = { category = key }, label = { Text(label, fontSize = 10.sp) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(ExpenseEntry(description = desc, amount = amount.toIntOrNull() ?: 0,
            category = category, recordedBy = admin)) },
            enabled = desc.isNotBlank() && amount.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(ts))
}
