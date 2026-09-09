package com.zerostress.manager

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onNavigate: (String) -> Unit) {
    val uid = FirebaseAuth.getInstance().uid
    var adminName by remember { mutableStateOf("Admin") }
    var totalPlayers by remember { mutableIntStateOf(0) }
    var activeMatches by remember { mutableIntStateOf(0) }
    var players by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var showPlayerList by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("players").document(uid).get()
            .addOnSuccessListener { if (it.exists()) adminName = it.getString("name") ?: "Admin" }
        FirebaseFirestore.getInstance().collection("players").get()
            .addOnSuccessListener { totalPlayers = it.size() }
        FirebaseFirestore.getInstance().collection("match_logs").get()
            .addOnSuccessListener { activeMatches = it.size() }
        FirebaseFirestore.getInstance().collection("players").get()
            .addOnSuccessListener { players = it.documents }
    }

    if (showAnnouncementDialog) {
        AnnouncementDialog(onDismiss = { showAnnouncementDialog = false })
    }
    if (showScheduleDialog) {
        ScheduleDialog(onDismiss = { showScheduleDialog = false })
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🛡️ Admin Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👋 $adminName", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Admin", color = ZSTextMuted, fontSize = 12.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalPlayers", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZSPrimary)
                    Text("Total Players", color = ZSTextMuted, fontSize = 12.sp)
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$activeMatches", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZSWarning)
                    Text("Match Logs", color = ZSTextMuted, fontSize = 12.sp)
                }
            }
        }

        val adminNav = listOf(
            "📋 Player Management" to { showPlayerList = !showPlayerList },
            "📝 Daily Input" to { onNavigate(Routes.DAILY_INPUT) },
            "📢 Announcements" to { showAnnouncementDialog = true },
            "🏆 Leaderboard" to { onNavigate(Routes.LEADERBOARD) },
            "💬 Chat" to { onNavigate(Routes.CHAT) },
            "🎙 Voice" to { onNavigate(Routes.VOICE) },
            "📅 Manage Seasons" to { onNavigate(Routes.MANAGE_SEASONS) },
            "📊 Player Stats" to { onNavigate(Routes.VIEW_ALL_PLAYERS_STATS) },
            "🔔 Send Notification" to { onNavigate(Routes.SEND_NOTIFICATION) },
            "⚙ Settings" to { onNavigate(Routes.SETTINGS) },
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(adminNav) { (label, action) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { action() },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = ZSCard)
                ) {
                    Text(label, modifier = Modifier.padding(16.dp), color = ZSText, fontSize = 15.sp)
                }
            }
            item {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onNavigate(Routes.LOGIN)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ZSDanger)
                ) { Text("Logout") }
            }
        }

        if (showPlayerList) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Player List", color = Color.White, fontWeight = FontWeight.Bold)
            players.forEach { doc ->
                val name = doc.getString("name") ?: "Unknown"
                val role = doc.getString("role") ?: "player"
                val status = doc.getString("status") ?: "pending"
                val score = doc.getLong("score") ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { /* TODO: player actions dialog */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = ZSCard)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$role • $status", color = ZSTextMuted, fontSize = 12.sp)
                        }
                        Text("$score", color = ZSPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementDialog(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📢 Broadcast Announcement") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Type announcement...") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    FirebaseFirestore.getInstance().collection("announcements").add(
                        mapOf("text" to text.trim(), "author" to "Admin", "timestamp" to System.currentTimeMillis())
                    )
                    onDismiss()
                }
            }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ScheduleDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    val uid = FirebaseAuth.getInstance().uid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📋 Add Match Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Match Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = timeStr, onValueChange = { timeStr = it }, label = { Text("Date & Time (2026-09-10 20:00)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type: Ranked / Custom / Tournament") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    var matchTimeMs = System.currentTimeMillis()
                    if (timeStr.isNotBlank()) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            sdf.parse(timeStr)?.let { matchTimeMs = it.time }
                        } catch (_: Exception) {
                            matchTimeMs = System.currentTimeMillis() + 86400000L
                        }
                    }
                    FirebaseFirestore.getInstance().collection("match_schedules").add(
                        mapOf(
                            "title" to title.trim(),
                            "description" to (type.ifBlank { "Custom Match" }),
                            "matchTime" to matchTimeMs,
                            "dateTime" to timeStr.ifBlank { "TBD" },
                            "status" to "Upcoming",
                            "type" to type.ifBlank { "Custom" },
                            "createdAt" to System.currentTimeMillis(),
                            "createdBy" to (uid ?: "")
                        )
                    )
                    onDismiss()
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
