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
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(phone: String, appViewModel: AppViewModel) {
    val friends by appViewModel.friends.collectAsState()
    val friendRequests by appViewModel.friendRequests.collectAsState()
    val searchResults by appViewModel.searchResults.collectAsState()
    val users by appViewModel.users.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { appViewModel.loadFriends(phone) }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Friends", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Accent)
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.length >= 2) appViewModel.searchUsers(it) else appViewModel.searchUsers("")
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search players by name...") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Accent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; appViewModel.searchUsers("") }) {
                        Icon(Icons.Filled.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent)
        )

        // Search results
        if (searchQuery.length >= 2 && searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Search Results", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    searchResults.filter { it.phone != phone && it.status == "confirmed" }.take(5).forEach { user ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("+880 ${user.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val alreadyFriend = friends.any {
                                (it.requesterPhone == phone && it.accepterPhone == user.phone) ||
                                (it.requesterPhone == user.phone && it.accepterPhone == phone)
                            }
                            if (alreadyFriend) {
                                Text("Friends", color = Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                IconButton(onClick = { appViewModel.sendFriendRequest(phone, user.phone) }) {
                                    Icon(Icons.Filled.PersonAdd, "Add Friend", tint = Accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Friends (${friends.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Requests (${friendRequests.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                if (friends.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No friends yet. Search to add players!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(friends) { friendship ->
                        val friendPhone = if (friendship.requesterPhone == phone) friendship.accepterPhone else friendship.requesterPhone
                        val friendName = users.find { it.phone == friendPhone }?.name ?: friendPhone

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp), color = Accent.copy(alpha = 0.15f)) {
                                        Box(contentAlignment = Alignment.Center) { Text(friendName.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(friendName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("+880 $friendPhone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { appViewModel.removeFriend(friendship.id) }) {
                                    Icon(Icons.Filled.PersonRemove, "Remove", tint = Danger)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (friendRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn {
                    items(friendRequests) { request ->
                        val requesterName = users.find { it.phone == request.requesterPhone }?.name ?: request.requesterPhone
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(requesterName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("wants to be your friend", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    IconButton(onClick = { appViewModel.acceptFriendRequest(request.id) }) {
                                        Icon(Icons.Filled.CheckCircle, "Accept", tint = Success)
                                    }
                                    IconButton(onClick = { appViewModel.removeFriend(request.id) }) {
                                        Icon(Icons.Filled.Cancel, "Decline", tint = Danger)
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
