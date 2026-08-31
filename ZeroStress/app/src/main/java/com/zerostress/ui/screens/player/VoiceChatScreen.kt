package com.zerostress.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.Player
import com.zerostress.data.model.VoiceChannel
import com.zerostress.data.model.VoiceParticipant
import com.zerostress.ui.theme.*

@Composable
fun VoiceChatScreen(
    channels: List<VoiceChannel>,
    currentChannel: VoiceChannel?,
    currentUser: Player,
    isMuted: Boolean,
    isDeafened: Boolean,
    onCreateChannel: (String, String) -> Unit,
    onJoinChannel: (String) -> Unit,
    onLeaveChannel: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onDeleteChannel: (String) -> Unit,
    onKickParticipant: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎙️ Voice Channels", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (currentChannel != null) {
                    FilledTonalButton(onClick = onLeaveChannel,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        )) {
                        Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Leave")
                    }
                }
            }
        }

        // Currently connected indicator
        AnimatedVisibility(visible = currentChannel != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OnlineGreen.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connected to ${currentChannel?.name ?: ""}",
                            fontWeight = FontWeight.Bold, color = OnlineGreen)
                        Text("${currentChannel?.participants?.size ?: 0} participants",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Channel list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Channels", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create Channel",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            items(channels) { channel ->
                VoiceChannelCard(
                    channel = channel,
                    currentUser = currentUser,
                    isInChannel = currentChannel?.id == channel.id,
                    onJoin = { onJoinChannel(channel.id) },
                    onDelete = { onDeleteChannel(channel.id) },
                    onKick = { phone -> onKickParticipant(channel.id, phone) }
                )
            }

            if (channels.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RecordVoiceOver, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No voice channels yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Create one to start talking!", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Bottom controls (when connected)
        AnimatedVisibility(visible = currentChannel != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isMuted) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isMuted) "Unmute" else "Mute", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    // Deafen button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onToggleDeafen,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isDeafened) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                if (isDeafened) Icons.Default.HeadphonesOff else Icons.Default.Headphones,
                                contentDescription = "Deafen",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isDeafened) "Undeafen" else "Deafen", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    // Hang up
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onLeaveChannel,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.CallEnd, "Leave",
                                modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onError)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Leave", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Create channel dialog
    if (showCreateDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type -> onCreateChannel(name, type); showCreateDialog = false }
        )
    }
}

@Composable
fun VoiceChannelCard(
    channel: VoiceChannel,
    currentUser: Player,
    isInChannel: Boolean,
    onJoin: () -> Unit,
    onDelete: () -> Unit,
    onKick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isCreator = channel.createdBy == currentUser.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInChannel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInChannel) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Channel header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Channel icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (channel.type) {
                                "squad" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                "tournament" -> Gold.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (channel.type) {
                            "squad" -> Icons.Default.Group
                            "tournament" -> Icons.Default.EmojiEvents
                            else -> Icons.Default.RecordVoiceOver
                        },
                        null,
                        tint = when (channel.type) {
                            "squad" -> MaterialTheme.colorScheme.secondary
                            "tournament" -> Gold
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "${channel.participants.size} connected • ${channel.type.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Join button or participant count
                if (channel.participants.isNotEmpty() && !isInChannel) {
                    FilledTonalButton(onClick = onJoin) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join")
                    }
                } else if (!isInChannel) {
                    Button(onClick = onJoin) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join")
                    }
                }

                if (isCreator || currentUser.isAdmin) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { expanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            // Participants list (when expanded or connected)
            if (channel.participants.isNotEmpty() && (expanded || isInChannel)) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                channel.participants.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaking indicator
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    if (participant.isSpeaking)
                                        Modifier.border(2.dp, OnlineGreen, CircleShape)
                                    else Modifier
                                )
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(participant.name.take(1).uppercase(), fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(participant.name, modifier = Modifier.weight(1f), fontSize = 14.sp)

                        // Status icons
                        if (participant.isMuted) {
                            Icon(Icons.Default.MicOff, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (participant.isDeafened) {
                            Icon(Icons.Default.HeadphonesOff, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Kick button for admins
                        if (currentUser.isAdmin && participant.phone != currentUser.phone) {
                            IconButton(onClick = { onKick(participant.phone) },
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PersonRemove, "Kick",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateChannelDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("squad") }
    val types = listOf("squad" to "Squad", "general" to "General", "tournament" to "Tournament")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Voice Channel") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Channel Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedType == key,
                            onClick = { selectedType = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onCreate(name, selectedType) },
                enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
