package com.zerostress.manager.ui.voice

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.data.VoiceService
import com.zerostress.manager.models.VoiceChannel
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLobbyScreen(phone: String, name: String, appViewModel: AppViewModel) {
    val context = LocalContext.current
    val channels by appViewModel.voiceChannels.collectAsState()
    var isInVoice by remember { mutableStateOf(false) }
    var currentChannelId by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Voice Channels", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
                Text("Free for life time", fontSize = 12.sp, color = Success)
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBg)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isInVoice) {
            // Active voice call UI
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Hearing, null, modifier = Modifier.size(48.dp), tint = Success)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connected to Voice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Success)
                    Text("Channel: $currentChannelId", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilledIconButton(
                            onClick = {
                                isMuted = !isMuted
                                // Toggle mute via service
                            },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isMuted) Danger else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                if (isMuted) "Unmute" else "Mute",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        FilledIconButton(
                            onClick = {
                                context.stopService(Intent(context, VoiceService::class.java))
                                appViewModel.leaveVoiceChannel(currentChannelId, phone)
                                isInVoice = false; currentChannelId = ""
                            },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Danger)
                        ) {
                            Icon(Icons.Filled.CallEnd, "Leave", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        // Channel list
        LazyColumn {
            items(channels) { channel ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VolumeUp, null, tint = if (channel.isActive) Success else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text("${channel.participants.size} participant(s)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!isInVoice) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, VoiceService::class.java).apply { putExtra("channel_id", channel.id) }
                                    context.startForegroundService(intent)
                                    appViewModel.joinVoiceChannel(channel.id, phone)
                                    isInVoice = true; currentChannelId = channel.id
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Success),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Headset, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Join", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var channelName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Voice Channel", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                OutlinedTextField(value = channelName, onValueChange = { channelName = it }, label = { Text("Channel Name") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (channelName.isNotBlank()) {
                        appViewModel.createVoiceChannel(channelName, phone)
                        showCreateDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
