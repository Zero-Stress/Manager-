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
import com.zerostress.manager.models.WeeklyChallenge
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(phone: String, role: String, appViewModel: AppViewModel) {
    val challenges by appViewModel.challenges.collectAsState()
    val dailyLogs by appViewModel.dailyLogs.collectAsState()
    val challengeProgress by appViewModel.challengeProgress.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val isAdmin = role == "admin"

    Column(modifier = Modifier.padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Challenges", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
            if (isAdmin) {
                Button(onClick = { showCreateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Challenge", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBg)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Active Challenges", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("My Progress", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("MVP Voting", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                val active = challenges.filter { it.isActive }
                if (active.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No active challenges.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(active) { challenge ->
                        val myProgress = challengeProgress.find { it.challengeId == challenge.id && it.phone == phone }
                        val progressPct = if (challenge.target > 0) ((myProgress?.currentValue ?: 0).toFloat() / challenge.target).coerceIn(0f, 1f) else 0f

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(challenge.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Surface(shape = RoundedCornerShape(8.dp), color = Warning.copy(alpha = 0.15f)) {
                                        Text("💰 ${challenge.rewardCoins}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Warning, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Text(challenge.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { progressPct }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = MaterialTheme.colorScheme.outline)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${myProgress?.currentValue ?: 0} / ${challenge.target}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(challenge.reward, fontSize = 11.sp, color = Success, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (challengeProgress.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Join a challenge to see progress!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(challengeProgress.filter { it.phone == phone }) { progress ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(progress.playerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${progress.currentValue}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Accent)
                            }
                        }
                    }
                }
            }
            2 -> {
                Text("Vote for MVP after a match", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                val users = appViewModel.users.collectAsState().value.filter { it.status == "confirmed" && it.phone != phone }
                LazyColumn {
                    items(users) { user ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("+880 ${user.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { appViewModel.castMVPVote("current", phone, user.phone, user.name) }) {
                                    Icon(Icons.Filled.EmojiEvents, "Vote MVP", tint = Gold)
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
        var target by remember { mutableStateOf("") }
        var rewardCoins by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("kills") }

        AlertDialog(onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Challenge", fontWeight = FontWeight.Bold, color = Accent) },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target (number)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rewardCoins, onValueChange = { rewardCoins = it }, label = { Text("Reward Coins") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && target.isNotBlank()) {
                        appViewModel.createChallenge(WeeklyChallenge(title = title, description = desc, type = type,
                            target = target.toIntOrNull() ?: 10, rewardCoins = rewardCoins.toIntOrNull() ?: 50, isActive = true, createdBy = phone))
                        showCreateDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
