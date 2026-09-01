package com.zerostress.manager.ui.player

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
import com.zerostress.manager.models.Poll
import com.zerostress.manager.models.LFGPost
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(phone: String, name: String, role: String, appViewModel: AppViewModel) {
    val polls by appViewModel.polls.collectAsState()
    val lfgPosts by appViewModel.lfgPosts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showLFGDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Community", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Polls", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("LFG Board", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                if (role == "admin") {
                    Button(onClick = { showPollDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Poll", fontWeight = FontWeight.Bold, color = DarkBg)
                    }
                }
                if (polls.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No polls yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(polls.filter { it.isActive }) { poll ->
                        val totalVotes = poll.votes.values.sum()
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(poll.question, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                poll.options.forEach { option ->
                                    val votes = poll.votes[option] ?: 0
                                    val pct = if (totalVotes > 0) (votes.toFloat() / totalVotes) * 100 else 0f
                                    Button(onClick = { appViewModel.votePoll(poll.id, option) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                        shape = RoundedCornerShape(10.dp)) {
                                        Text("$option  ($votes - ${String.format("%.0f", pct)}%)", fontSize = 13.sp)
                                    }
                                }
                                Text("Total votes: $totalVotes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
            1 -> {
                Button(onClick = { showLFGDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.GroupAdd, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp))
                    Text("Looking for Players", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
                if (lfgPosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No LFG posts. Be the first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(lfgPosts) { post ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(post.playerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Surface(shape = RoundedCornerShape(8.dp), color = Accent.copy(alpha = 0.1f)) {
                                        Text(post.lookingFor, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Text(post.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(post.skillLevel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (post.phone == phone || role == "admin") {
                                        IconButton(onClick = { appViewModel.deleteLFGPost(post.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Filled.Close, null, tint = Danger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPollDialog) {
        var question by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }
        var opt3 by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showPollDialog = false },
            title = { Text("Create Poll", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                Column {
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option 1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option 2") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option 3 (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val opts = listOf(opt1, opt2, opt3).filter { it.isNotBlank() }
                    if (question.isNotBlank() && opts.size >= 2) {
                        appViewModel.createPoll(Poll(question = question, options = opts, createdBy = phone))
                        showPollDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showPollDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLFGDialog) {
        var msg by remember { mutableStateOf("") }
        var lookingFor by remember { mutableStateOf("squad") }
        var skillLevel by remember { mutableStateOf("intermediate") }
        AlertDialog(onDismissRequest = { showLFGDialog = false },
            title = { Text("Looking for Players", fontWeight = FontWeight.Bold, color = Success) },
            text = {
                Column {
                    OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("squad", "duo", "custom match").forEach { opt ->
                            FilterChip(selected = lookingFor == opt, onClick = { lookingFor = opt }, label = { Text(opt) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("beginner", "intermediate", "pro").forEach { opt ->
                            FilterChip(selected = skillLevel == opt, onClick = { skillLevel = opt }, label = { Text(opt) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (msg.isNotBlank()) {
                        appViewModel.createLFGPost(LFGPost(phone = phone, playerName = name, message = msg, lookingFor = lookingFor, skillLevel = skillLevel))
                        showLFGDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Success)) { Text("Post") }
            },
            dismissButton = { TextButton(onClick = { showLFGDialog = false }) { Text("Cancel") } }
        )
    }
}
