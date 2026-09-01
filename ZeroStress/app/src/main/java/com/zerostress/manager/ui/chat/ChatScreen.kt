package com.zerostress.manager.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.manager.models.ChatMessage
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(phone: String, name: String, role: String, appViewModel: AppViewModel) {
    val messages by appViewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isAdmin = role == "admin"

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat header
        TopAppBar(
            title = { Text("Team Chat", fontWeight = FontWeight.Bold) },
            navigationIcon = { },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            actions = {
                if (isAdmin) {
                    IconButton(onClick = { appViewModel.clearChat() }) {
                        Icon(Icons.Filled.DeleteSweep, "Clear Chat", tint = Danger)
                    }
                }
            }
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == phone
                val isSystem = msg.senderId == "system"

                if (isSystem) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(msg.text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isMe) Accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                )
                                .padding(12.dp)
                        ) {
                            if (!isMe) {
                                Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accent)
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(msg.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            val time = try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)) } catch (e: Exception) { "" }
                            Text(time, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }

        // Input bar
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            appViewModel.sendChatMessage(
                                ChatMessage(senderId = phone, senderName = name, text = inputText.trim(), isAdmin = isAdmin)
                            )
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, "Send", tint = Accent)
                }
            }
        }
    }
}
