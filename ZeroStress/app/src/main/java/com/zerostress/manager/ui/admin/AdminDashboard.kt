package com.zerostress.manager.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zerostress.manager.data.PreferenceManager
import com.zerostress.manager.navigation.Screen
import com.zerostress.manager.ui.theme.*
import com.zerostress.manager.ui.player.FriendsScreen
import com.zerostress.manager.ui.player.PlayerComparisonScreen
import com.zerostress.manager.ui.player.PerformanceTrendsScreen
import com.zerostress.manager.ui.player.AchievementsScreen
import com.zerostress.manager.ui.player.RankLevelScreen
import com.zerostress.manager.ui.player.DailyRewardsScreen
import com.zerostress.manager.ui.player.ChallengesScreen
import com.zerostress.manager.ui.player.CommunityScreen
import com.zerostress.manager.viewmodel.AppViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    phone: String,
    name: String,
    role: String,
    prefs: PreferenceManager,
    navController: NavController,
    onLogout: () -> Unit,
    appViewModel: AppViewModel = viewModel()
) {
    val isDark by prefs.isDarkTheme.collectAsState(initial = true)
    val announcements by appViewModel.announcements.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val isAdmin = role == "admin"

    val tabs = if (isAdmin) listOf(
        "Rank", "Rewards", "Players", "Daily", "Daily LB", "Weekly", "Weekly LB", "Profile",
        "Friends", "Compare", "Trends", "Badges", "Seasons", "Challenges", "Community",
        "Analytics", "Chat", "Voice", "Schedule", "Announce"
    ) else listOf(
        "Rank", "Rewards", "Daily LB", "Weekly LB", "Profile",
        "Friends", "Compare", "Trends", "Badges", "Seasons", "Challenges", "Community",
        "Chat", "Voice", "Schedule"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome,", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.outline
                    ) {
                        Text(role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Row {
                    IconButton(onClick = { navController.navigate(Screen.Chat.route) }) {
                        Icon(Icons.Filled.Chat, "Chat", tint = Accent)
                    }
                    IconButton(onClick = { navController.navigate(Screen.VoiceLobby.route) }) {
                        Icon(Icons.Filled.Headset, "Voice", tint = Accent)
                    }
                    IconButton(onClick = { navController.navigate(Screen.MatchSchedule.route) }) {
                        Icon(Icons.Filled.CalendarMonth, "Schedule", tint = Accent)
                    }
                    IconButton(onClick = {
                        MainScope().launch { prefs.setDarkTheme(!isDark) }
                    }) {
                        Icon(
                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            "Theme", tint = Accent
                        )
                    }
                    IconButton(onClick = {
                        MainScope().launch { prefs.clearSession() }
                        onLogout()
                    }) {
                        Icon(Icons.Filled.Logout, "Logout", tint = Danger)
                    }
                }
            }
        }

        // Announcement banner
        if (announcements.isNotEmpty()) {
            val latest = announcements.first()
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Campaign, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(latest.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                }
            }
        }

        // Tab navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Accent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Normal) })
            }
        }

        // Content
        when (tabs[selectedTab]) {
            "Players" -> if (isAdmin) PlayerManagementScreen(appViewModel = appViewModel)
            "Daily Input" -> if (isAdmin) DailyInputScreen(appViewModel = appViewModel)
            "Daily LB" -> DailyLeaderboardScreen(appViewModel = appViewModel)
            "Weekly" -> if (isAdmin) WeeklySummaryScreen(appViewModel = appViewModel)
            "Weekly LB" -> WeeklyLeaderboardScreen(appViewModel = appViewModel)
            "Profile" -> PlayerProfileScreen(phone = phone, name = name, appViewModel = appViewModel)
            "Chat" -> ChatScreen(phone = phone, name = name, role = role, appViewModel = appViewModel)
            "Voice" -> VoiceLobbyScreen(phone = phone, name = name, appViewModel = appViewModel)
            "Friends" -> FriendsScreen(phone = phone, appViewModel = appViewModel)
            "Compare" -> PlayerComparisonScreen(myPhone = phone, myName = name, appViewModel = appViewModel)
            "Trends" -> PerformanceTrendsScreen(phone = phone, appViewModel = appViewModel)
            "Badges" -> AchievementsScreen(phone = phone, appViewModel = appViewModel)
            "Seasons" -> SeasonScreen(phone = phone, role = role, appViewModel = appViewModel)
            "Schedule" -> MatchScheduleScreen(role = role, appViewModel = appViewModel)
            "Announce" -> if (isAdmin) AnnouncementsScreen(appViewModel = appViewModel)
            "Rank" -> RankLevelScreen(phone = phone, name = name, appViewModel = appViewModel)
            "Rewards" -> DailyRewardsScreen(phone = phone, appViewModel = appViewModel)
            "Challenges" -> ChallengesScreen(phone = phone, role = role, appViewModel = appViewModel)
            "Community" -> CommunityScreen(phone = phone, name = name, role = role, appViewModel = appViewModel)
            "Analytics" -> if (isAdmin) AdminAnalyticsScreen(phone = phone, appViewModel = appViewModel)
        }
    }
}
