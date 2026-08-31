package com.zerostress.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.screens.admin.*
import com.zerostress.ui.screens.player.*
import com.zerostress.viewmodel.AppViewModel
import com.zerostress.viewmodel.VoiceChatViewModel

// Player nav items
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Leaderboard : Screen("leaderboard", "Ranks", Icons.Default.EmojiEvents)
    data object VoiceChat : Screen("voice", "Voice", Icons.Default.RecordVoiceOver)
    data object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

// Admin nav items
sealed class AdminScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : AdminScreen("admin_dashboard", "Players", Icons.Default.People)
    data object DailyInput : AdminScreen("admin_daily", "Input", Icons.Default.Input)
    data object Announcements : AdminScreen("admin_announce", "News", Icons.Default.Campaign)
}

@Composable
fun MainNavigation(
    currentUser: Player,
    viewModel: AppViewModel,
    voiceViewModel: VoiceChatViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isAdmin = currentUser.isAdmin
    val players by viewModel.players.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val channels by voiceViewModel.channels.collectAsState()
    val currentChannel by voiceViewModel.currentChannel.collectAsState()
    val isMuted by voiceViewModel.isMuted.collectAsState()
    val isDeafened by voiceViewModel.isDeafened.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                if (isAdmin) {
                    // Admin: 5 tabs
                    val adminItems = listOf(
                        AdminScreen.Dashboard, AdminScreen.DailyInput, AdminScreen.Announcements
                    )
                    adminItems.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(screen.icon, null) },
                            label = { Text(screen.label, fontSize = 10.sp) }
                        )
                    }
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Screen.Leaderboard.icon, null) },
                        label = { Text("Ranks", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Screen.VoiceChat.icon, null) },
                        label = { Text("Voice", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        icon = { Icon(Screen.Chat.icon, null) },
                        label = { Text("Chat", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 6,
                        onClick = { selectedTab = 6 },
                        icon = { Icon(Screen.Profile.icon, null) },
                        label = { Text("Profile", fontSize = 10.sp) }
                    )
                } else {
                    // Player: 4 tabs
                    listOf(Screen.Leaderboard, Screen.VoiceChat, Screen.Chat, Screen.Profile)
                        .forEachIndexed { index, screen ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(screen.icon, null) },
                                label = { Text(screen.label, fontSize = 10.sp) }
                            )
                        }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> AdminDashboard(
                        players = players,
                        onApprove = viewModel::approvePlayer,
                        onReject = viewModel::rejectPlayer,
                        onDelete = viewModel::deletePlayer,
                        onAddPlayer = { /* TODO */ }
                    )
                    1 -> DailyInputScreen(players = players, onSave = viewModel::addDailyRecord)
                    2 -> AnnouncementsScreen(
                        announcements = announcements,
                        onPost = { viewModel.postAnnouncement(it, currentUser.name) },
                        onDelete = viewModel::deleteAnnouncement,
                        isAdmin = true
                    )
                    3 -> LeaderboardScreen(leaderboard = leaderboard)
                    4 -> VoiceChatScreen(
                        channels = channels,
                        currentChannel = currentChannel,
                        currentUser = currentUser,
                        isMuted = isMuted,
                        isDeafened = isDeafened,
                        onCreateChannel = { name, type -> voiceViewModel.createChannel(name, type, currentUser.name) },
                        onJoinChannel = { voiceViewModel.joinChannel(it, currentUser) },
                        onLeaveChannel = { voiceViewModel.leaveChannel() },
                        onToggleMute = { voiceViewModel.toggleMute() },
                        onToggleDeafen = { voiceViewModel.toggleDeafen() },
                        onDeleteChannel = { voiceViewModel.deleteChannel(it) },
                        onKickParticipant = { chId, phone -> voiceViewModel.kickParticipant(chId, phone) }
                    )
                    5 -> ChatScreen(
                        messages = chatMessages,
                        currentUserName = currentUser.name,
                        isAdmin = true,
                        onSend = { viewModel.sendMessage(currentUser.name, it, true) },
                        onReply = { msg, sender -> viewModel.replyToMessage(currentUser.name, msg, sender, true) },
                        onDelete = { viewModel.deleteMessage(it) },
                        onClearChat = { viewModel.clearChat() }
                    )
                    6 -> ProfileScreen(
                        player = currentUser, dailyLogs = dailyLogs,
                        leaderboard = leaderboard, onLogout = onLogout
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> LeaderboardScreen(leaderboard = leaderboard)
                    1 -> VoiceChatScreen(
                        channels = channels,
                        currentChannel = currentChannel,
                        currentUser = currentUser,
                        isMuted = isMuted,
                        isDeafened = isDeafened,
                        onCreateChannel = { name, type -> voiceViewModel.createChannel(name, type, currentUser.name) },
                        onJoinChannel = { voiceViewModel.joinChannel(it, currentUser) },
                        onLeaveChannel = { voiceViewModel.leaveChannel() },
                        onToggleMute = { voiceViewModel.toggleMute() },
                        onToggleDeafen = { voiceViewModel.toggleDeafen() },
                        onDeleteChannel = { },
                        onKickParticipant = { _, _ -> }
                    )
                    2 -> ChatScreen(
                        messages = chatMessages,
                        currentUserName = currentUser.name,
                        isAdmin = false,
                        onSend = { viewModel.sendMessage(currentUser.name, it, false) },
                        onReply = { msg, sender -> viewModel.replyToMessage(currentUser.name, msg, sender, false) },
                        onDelete = { },
                        onClearChat = { }
                    )
                    3 -> ProfileScreen(
                        player = currentUser, dailyLogs = dailyLogs,
                        leaderboard = leaderboard, onLogout = onLogout
                    )
                }
            }
        }
    }
}
