package com.zerostress.manager.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zerostress.manager.data.PreferenceManager
import com.zerostress.manager.ui.auth.LoginScreen
import com.zerostress.manager.ui.auth.RegisterScreen
import com.zerostress.manager.ui.admin.AdminDashboard
import com.zerostress.manager.ui.admin.PlayerManagementScreen
import com.zerostress.manager.ui.admin.DailyInputScreen
import com.zerostress.manager.ui.admin.WeeklySummaryScreen
import com.zerostress.manager.ui.admin.AnnouncementsScreen
import com.zerostress.manager.ui.player.DailyLeaderboardScreen
import com.zerostress.manager.ui.player.WeeklyLeaderboardScreen
import com.zerostress.manager.ui.player.PlayerProfileScreen
import com.zerostress.manager.ui.chat.ChatScreen
import com.zerostress.manager.ui.voice.VoiceLobbyScreen
import com.zerostress.manager.ui.schedule.MatchScheduleScreen
import com.zerostress.manager.ui.player.FriendsScreen
import com.zerostress.manager.ui.player.PlayerComparisonScreen
import com.zerostress.manager.ui.player.PerformanceTrendsScreen
import com.zerostress.manager.ui.player.AchievementsScreen
import com.zerostress.manager.ui.admin.SeasonScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object PlayerManagement : Screen("player_management")
    object DailyInput : Screen("daily_input")
    object DailyLeaderboard : Screen("daily_leaderboard")
    object WeeklySummary : Screen("weekly_summary")
    object WeeklyLeaderboard : Screen("weekly_leaderboard")
    object Profile : Screen("profile")
    object Announcements : Screen("announcements")
    object Chat : Screen("chat")
    object VoiceLobby : Screen("voice_lobby")
    object MatchSchedule : Screen("match_schedule")
    object Friends : Screen("friends")
    object Compare : Screen("compare")
    object Trends : Screen("trends")
    object Achievements : Screen("achievements")
    object Seasons : Screen("seasons")
}

@Composable
fun AppNavigation(prefs: PreferenceManager) {
    val navController = rememberNavController()
    val isLoggedIn by prefs.isLoggedIn.collectAsState(initial = false)
    val sessionPhone by prefs.sessionPhone.collectAsState(initial = "")
    val sessionName by prefs.sessionName.collectAsState(initial = "")
    val sessionRole by prefs.sessionRole.collectAsState(initial = "player")

    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                prefs = prefs,
                onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onGoToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Dashboard.route) {
            AdminDashboard(
                phone = sessionPhone,
                name = sessionName,
                role = sessionRole,
                prefs = prefs,
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Screen.PlayerManagement.route) {
            PlayerManagementScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DailyInput.route) {
            DailyInputScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DailyLeaderboard.route) {
            DailyLeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WeeklySummary.route) {
            WeeklySummaryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WeeklyLeaderboard.route) {
            WeeklyLeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            PlayerProfileScreen(phone = sessionPhone, name = sessionName, onBack = { navController.popBackStack() })
        }
        composable(Screen.Announcements.route) {
            AnnouncementsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Chat.route) {
            ChatScreen(phone = sessionPhone, name = sessionName, role = sessionRole, onBack = { navController.popBackStack() })
        }
        composable(Screen.VoiceLobby.route) {
            VoiceLobbyScreen(phone = sessionPhone, name = sessionName, onBack = { navController.popBackStack() })
        }
        composable(Screen.MatchSchedule.route) {
            MatchScheduleScreen(role = sessionRole, onBack = { navController.popBackStack() })
        }
        composable(Screen.Friends.route) {
            FriendsScreen(phone = sessionPhone, appViewModel = viewModel())
        }
        composable(Screen.Compare.route) {
            PlayerComparisonScreen(myPhone = sessionPhone, myName = sessionName, appViewModel = viewModel())
        }
        composable(Screen.Trends.route) {
            PerformanceTrendsScreen(phone = sessionPhone, appViewModel = viewModel())
        }
        composable(Screen.Achievements.route) {
            AchievementsScreen(phone = sessionPhone, appViewModel = viewModel())
        }
        composable(Screen.Seasons.route) {
            SeasonScreen(phone = sessionPhone, role = sessionRole, appViewModel = viewModel())
        }
    }
}
