package com.zerostress.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.ChatMessage
import com.zerostress.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentUserName: String,
    isAdmin: Boolean,
    onSend: (String) -> Unit,
    onReply: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Pair<String, String>?>(null) } // sender, message
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Group messages by date
    val groupedMessages = remember(messages) {
        messages.groupBy { dayFormat.format(Date(it.timestamp)) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // WhatsApp-style header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online indicator
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Team Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${messages.size} messages", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                if (isAdmin) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, "Clear Chat",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Messages
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No messages yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("Start the conversation!", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                groupedMessages.forEach { (date, dayMessages) ->
                    item {
                        // Date separator
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(date, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }
                    items(dayMessages) { msg ->
                        val isMe = msg.sender == currentUserName
                        SwipeableMessage(
                            message = msg,
                            isMe = isMe,
                            dateFormat = dateFormat,
                            onReply = { replyingTo = msg.sender to msg.message },
                            onDelete = { if (isAdmin || isMe) onDelete(msg.id) },
                            isAdmin = isAdmin
                        )
                    }
                }
            }
        }

        // Reply preview
        AnimatedVisibility(visible = replyingTo != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(replyingTo?.first ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(replyingTo?.second ?: "", fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Input bar (WhatsApp style)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji button placeholder
                IconButton(onClick = { /* TODO: Emoji picker */ }) {
                    Icon(Icons.Default.EmojiEmotions, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Message", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Send button
                AnimatedVisibility(visible = inputText.isNotBlank()) {
                    FilledIconButton(
                        onClick = {
                            if (replyingTo != null) {
                                onReply(inputText, replyingTo!!.first)
                                replyingTo = null
                            } else {
                                onSend(inputText)
                            }
                            inputText = ""
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }

                AnimatedVisibility(visible = inputText.isBlank()) {
                    IconButton(onClick = { /* TODO: Voice note */ }) {
                        Icon(Icons.Default.Mic, "Voice Note",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Clear chat dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Chat?") },
            text = { Text("This will permanently delete all messages. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { showClearDialog = false; onClearChat() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SwipeableMessage(
    message: ChatMessage,
    isMe: Boolean,
    dateFormat: SimpleDateFormat,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean
) {
    var swiped by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalGestures { _, _ ->
                    swiped = true
                }
            }
    ) {
        // Message bubble
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Sender name (for others)
                if (!isMe) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message.sender, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (message.isAdmin) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                        if (message.isAdmin) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADMIN", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                // Reply reference
                if (message.message.contains("↩")) {
                    val parts = message.message.split("↩", limit = 2)
                    if (parts.size == 2) {
                        Text(parts[0].trim(), fontSize = 11.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .padding(4.dp))
                        Text(parts[1].trim(), fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Text(message.message, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }

                // Timestamp + read receipt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateFormat.format(Date(message.timestamp)),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Swipe action overlay
        AnimatedVisibility(visible = swiped, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { swiped = false; onReply() }) {
                    Icon(Icons.Default.Reply, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply")
                }
                if (isAdmin || isMe) {
                    TextButton(onClick = { swiped = false; onDelete() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}
