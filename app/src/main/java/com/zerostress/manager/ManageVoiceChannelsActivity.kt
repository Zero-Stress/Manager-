package com.zerostress.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.ui.theme.*

class ManageVoiceChannelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeroStressTheme { ManageVoiceChannelsScreen() } }
    }
}

@Composable
fun ManageVoiceChannelsScreen() {
    val db = FirebaseFirestore.getInstance()
    var channels by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("voice_channels").addSnapshotListener { s, _ ->
            if (s != null) channels = s.documents
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ New Voice Channel") },
            text = {
                OutlinedTextField(value = channelName, onValueChange = { channelName = it }, label = { Text("Channel Name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (channelName.isNotBlank()) {
                        db.collection("voice_channels").add(
                            mapOf("name" to channelName.trim(), "status" to "Active", "createdAt" to System.currentTimeMillis())
                        )
                        channelName = ""
                        showAddDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🎙 Manage Channels", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { showAddDialog = true }) { Text("+ Add") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels) { doc ->
                val name = doc.getString("name") ?: "Channel"
                val status = doc.getString("status") ?: "Active"
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = ZSCard)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔊 $name", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Status: $status", color = ZSTextMuted, fontSize = 12.sp)
                        }
                        Row {
                            TextButton(onClick = {
                                val newStatus = if (status == "Active") "Locked" else "Active"
                                doc.reference.update("status", newStatus)
                            }) { Text(if (status == "Active") "🔒 Lock" else "🔓 Unlock", color = ZSWarning, fontSize = 11.sp) }
                            TextButton(onClick = { doc.reference.delete() }) { Text("🗑", color = ZSDanger) }
                        }
                    }
                }
            }
        }
    }
}
