package com.zerostress.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    posts: List<TeamFeedPost>,
    stories: List<TeamStory>,
    players: List<Player>,
    currentUserName: String,
    onCreatePost: (TeamFeedPost) -> Unit,
    onLikePost: (String) -> Unit,
    onComment: (String, TeamComment) -> Unit,
    onPostStory: (TeamStory) -> Unit,
    onDeletePost: (String) -> Unit,
    isAdmin: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewPostDialog by remember { mutableStateOf(false) }
    var showStoryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("📱 Team Social") },
            actions = {
                IconButton(onClick = { showNewPostDialog = true }) { Icon(Icons.Default.Add, "New Post") }
                IconButton(onClick = { showStoryDialog = true }) { Icon(Icons.Default.CameraAlt, "Story") }
            })

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("📰 Feed") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("📖 Stories") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("👥 Members") })
        }

        when (selectedTab) {
            0 -> FeedTab(posts, currentUserName, isAdmin, onLikePost, onComment, onDeletePost)
            1 -> StoriesTab(stories)
            2 -> MembersTab(players)
        }
    }

    if (showNewPostDialog) {
        NewPostDialog(currentUserName, onDismiss = { showNewPostDialog = false },
            onPost = { onCreatePost(it); showNewPostDialog = false })
    }
    if (showStoryDialog) {
        NewStoryDialog(currentUserName, onDismiss = { showStoryDialog = false },
            onPost = { onPostStory(it); showStoryDialog = false })
    }
}

@Composable
private fun FeedTab(posts: List<TeamFeedPost>, currentUser: String, isAdmin: Boolean,
                    onLike: (String) -> Unit, onComment: (String, TeamComment) -> Unit, onDelete: (String) -> Unit) {
    var commentingOn by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (posts.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📰", fontSize = 48.sp)
                        Text("No posts yet", fontWeight = FontWeight.Bold)
                        Text("Be the first to share something!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(posts) { post ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ZSBlue), contentAlignment = Alignment.Center) {
                            Text(post.authorName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.authorName, fontWeight = FontWeight.Bold)
                            Text(formatTimestamp(post.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (post.authorName == currentUser || isAdmin) {
                            IconButton(onClick = { onDelete(post.id) }) { Icon(Icons.Default.Delete, null, tint = ZSRed, modifier = Modifier.size(18.dp)) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(post.content, lineHeight = 22.sp)

                    if (post.type == "highlight") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2D1B)), shape = RoundedCornerShape(8.dp)) {
                            Text("⭐ Highlight", modifier = Modifier.padding(8.dp), color = ZSGreen, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val liked = post.likes.contains(currentUser)
                        Row(modifier = Modifier.clickable { onLike(post.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (liked) "❤️" else "🤍")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.likes.size}", fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.clickable { commentingOn = if (commentingOn == post.id) null else post.id },
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("💬")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.comments.size}", fontSize = 13.sp)
                        }
                    }

                    if (post.comments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        post.comments.takeLast(3).forEach { comment ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(comment.text, fontSize = 12.sp)
                            }
                        }
                    }

                    if (commentingOn == post.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = commentText, onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f), placeholder = { Text("Comment...") },
                                shape = RoundedCornerShape(20.dp), singleLine = true)
                            IconButton(onClick = {
                                if (commentText.isNotBlank()) {
                                    onComment(post.id, TeamComment(authorName = currentUser, text = commentText))
                                    commentText = ""; commentingOn = null
                                }
                            }) { Icon(Icons.Default.Send, null, tint = ZSBlue) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoriesTab(stories: List<TeamStory>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (stories.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖", fontSize = 48.sp)
                        Text("No stories yet", fontWeight = FontWeight.Bold)
                        Text("Stories disappear after 24 hours", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(stories) { story ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ZSPurple), contentAlignment = Alignment.Center) {
                            Text(story.authorName.first().toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(story.authorName, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(when(story.type) { "match" -> "🎮"; "celebration" -> "🎉"; else -> "📝" }, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(story.content, lineHeight = 20.sp)
                    val hoursLeft = ((story.expiresAt - System.currentTimeMillis()) / 3600000).coerceAtLeast(0)
                    Text("⏰ Expires in ${hoursLeft}h", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MembersTab(players: List<Player>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("👥 Team Members (${players.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(players) { player ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (player.isOnline) ZSGreen else Color.Gray), contentAlignment = Alignment.Center) {
                        Text(player.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(player.name, fontWeight = FontWeight.Bold)
                        Text(player.role.uppercase(), fontSize = 11.sp, color = if (player.isAdmin) ZSOrange else ZSBlue)
                    }
                    Text(if (player.isOnline) "🟢" else "⚫", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NewPostDialog(author: String, onDismiss: () -> Unit, onPost: (TeamFeedPost) -> Unit) {
    var content by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf("text") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("New Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = content, onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("What's happening?") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = postType == "text", onClick = { postType = "text" }, label = { Text("📝 Text") })
                    FilterChip(selected = postType == "highlight", onClick = { postType = "highlight" }, label = { Text("⭐ Highlight") })
                    FilterChip(selected = postType == "announcement", onClick = { postType = "announcement" }, label = { Text("📢 News") })
                }
            }
        },
        confirmButton = { Button(onClick = { onPost(TeamFeedPost(authorName = author, content = content, type = postType)) },
            enabled = content.isNotBlank()) { Text("Post") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NewStoryDialog(author: String, onDismiss: () -> Unit, onPost: (TeamStory) -> Unit) {
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("text") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("New Story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = content, onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp), placeholder = { Text("Share a story...") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "text", onClick = { type = "text" }, label = { Text("📝") })
                    FilterChip(selected = type == "match", onClick = { type = "match" }, label = { Text("🎮") })
                    FilterChip(selected = type == "celebration", onClick = { type = "celebration" }, label = { Text("🎉") })
                }
                Text("⏰ Expires in 24 hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onPost(TeamStory(authorName = author, content = content, type = type)) },
            enabled = content.isNotBlank()) { Text("Post Story") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(ts))
}
