package com.zerostress.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.screens.admin.*
import com.zerostress.ui.screens.player.*
import com.zerostress.viewmodel.AppViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Leaderboard : Screen("leaderboard", "Ranks", Icons.Default.EmojiEvents)
    data object Analytics : Screen("analytics", "Stats", Icons.Default.Analytics)
    data object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

sealed class AdminScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : AdminScreen("admin_dashboard", "Players", Icons.Default.People)
    data object DailyInput : AdminScreen("admin_daily", "Input", Icons.Default.Input)
    data object Announcements : AdminScreen("admin_announce", "News", Icons.Default.Campaign)
}

@Composable
fun MainNavigation(
    currentUser: Player,
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isAdmin = currentUser.isAdmin
    val players by viewModel.players.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    val playerTabs = listOf(Screen.Leaderboard, Screen.Analytics, Screen.Chat, Screen.Profile)
    val adminTabs = listOf(
        AdminScreen.Dashboard, AdminScreen.DailyInput, AdminScreen.Announcements
    )

    // Add player tabs for admin too
    val allTabs = if (isAdmin) {
        listOf(
            Screen.Leaderboard, Screen.Analytics, Screen.Chat, Screen.Profile
        )
    } else playerTabs

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                if (isAdmin) {
                    // Admin tabs
                    adminTabs.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(screen.icon, null) },
                            label = { Text(screen.label, fontSize = 10.sp) }
                        )
                    }
                    // Plus player tabs
                    NavigationBarItem(
                        selected = selectedTab == adminTabs.size,
                        onClick = { selectedTab = adminTabs.size },
                        icon = { Icon(Screen.Leaderboard.icon, null) },
                        label = { Text("Ranks", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == adminTabs.size + 1,
                        onClick = { selectedTab = adminTabs.size + 1 },
                        icon = { Icon(Screen.Profile.icon, null) },
                        label = { Text("Profile", fontSize = 10.sp) }
                    )
                } else {
                    allTabs.forEachIndexed { index, screen ->
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
                        onAddPlayer = { /* TODO: Show add player dialog */ }
                    )
                    1 -> DailyInputScreen(
                        players = players,
                        onSave = viewModel::addDailyRecord
                    )
                    2 -> AnnouncementsScreen(
                        announcements = announcements,
                        onPost = { viewModel.postAnnouncement(it, currentUser.name) },
                        onDelete = viewModel::deleteAnnouncement,
                        isAdmin = true
                    )
                    3 -> LeaderboardScreen(leaderboard = leaderboard)
                    4 -> ProfileScreen(
                        player = currentUser,
                        dailyLogs = dailyLogs,
                        leaderboard = leaderboard,
                        onLogout = onLogout
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> LeaderboardScreen(leaderboard = leaderboard)
                    1 -> AnnouncementsScreen(
                        announcements = announcements,
                        onPost = {}, onDelete = {}, isAdmin = false
                    )
                    2 -> ChatScreen(
                        messages = chatMessages,
                        currentUserName = currentUser.name,
                        isAdmin = false,
                        onSend = { viewModel.sendMessage(currentUser.name, it, false) },
                        onClearChat = {}
                    )
                    3 -> ProfileScreen(
                        player = currentUser,
                        dailyLogs = dailyLogs,
                        leaderboard = leaderboard,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
