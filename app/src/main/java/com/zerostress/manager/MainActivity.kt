package com.zerostress.manager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zerostress.manager.fcm.ZSFCMService
import com.zerostress.manager.ui.theme.ZeroStressTheme

/**
 * Single-Activity architecture: every screen is a route in the NavHost.
 */
class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) ZSFCMService.saveTokenToFirestore(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            ZeroStressTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    ZeroStressNavGraph()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ZSFCMService.saveTokenToFirestore(this)
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PLAYER_DASHBOARD = "player_dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val LEADERBOARD = "leaderboard"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val PROFILE = "profile"
    const val FRIENDS = "friends"
    const val SCHEDULE = "schedule"
    const val SEASONS = "seasons"
    const val ACHIEVEMENTS = "achievements"
    const val SUBMIT_MATCH = "submit_match"
    const val ANNOUNCEMENTS = "announcements"
    const val DAILY_INPUT = "daily_input"
    const val PERFORMANCE = "performance"
    const val PERFORMANCE_GRAPHS = "performance_graphs"
    const val SETTINGS = "settings"
    const val PLAYER_TITLES = "player_titles"
    const val DAILY_CHALLENGES = "daily_challenges"
    const val BATTLE_PASS = "battle_pass"
    const val DAILY_LOGIN_REWARDS = "daily_login_rewards"
    const val SEND_NOTIFICATION = "send_notification"
    const val MANAGE_SEASONS = "manage_seasons"
    const val MANAGE_VOICE_CHANNELS = "manage_voice_channels"
    const val VIEW_ALL_PLAYERS_STATS = "view_all_players_stats"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun ZeroStressNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinish = { role ->
                val target = when (role) {
                    "admin" -> Routes.ADMIN_DASHBOARD
                    "player" -> Routes.PLAYER_DASHBOARD
                    else -> Routes.LOGIN
                }
                navController.navigate(target) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { role ->
                    val target = if (role == "admin") Routes.ADMIN_DASHBOARD else Routes.PLAYER_DASHBOARD
                    navController.navigate(target) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() }
            )
        }
        composable(Routes.PLAYER_DASHBOARD) {
            PlayerDashboardScreen(onNavigate = { route ->
                navController.navigate(route) {
                    if (route == Routes.LOGIN) popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(onNavigate = { route ->
                navController.navigate(route) {
                    if (route == Routes.LOGIN) popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(Routes.LEADERBOARD) { LeaderboardScreen() }
        composable(Routes.CHAT) { ChatScreen() }
        composable(Routes.VOICE) { VoiceScreen() }
        composable(Routes.PROFILE) { ProfileScreen() }
        composable(Routes.FRIENDS) { FriendsScreen() }
        composable(Routes.SCHEDULE) { ScheduleScreen() }
        composable(Routes.SEASONS) { SeasonScreen() }
        composable(Routes.ACHIEVEMENTS) { AchievementsScreen() }
        composable(Routes.SUBMIT_MATCH) { SubmitMatchScreen() }
        composable(Routes.ANNOUNCEMENTS) { AnnouncementsScreen() }
        composable(Routes.DAILY_INPUT) { DailyInputScreen() }
        composable(Routes.PERFORMANCE) { PerformanceScreen() }
        composable(Routes.PERFORMANCE_GRAPHS) { PerformanceGraphsScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(onLoggedOut = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(Routes.PLAYER_TITLES) { PlayerTitlesScreen() }
        composable(Routes.DAILY_CHALLENGES) { DailyChallengesScreen() }
        composable(Routes.BATTLE_PASS) { BattlePassScreen() }
        composable(Routes.DAILY_LOGIN_REWARDS) { DailyLoginRewardsScreen() }
        composable(Routes.SEND_NOTIFICATION) { SendNotificationScreen() }
        composable(Routes.MANAGE_SEASONS) { ManageSeasonsScreen() }
        composable(Routes.MANAGE_VOICE_CHANNELS) { ManageVoiceChannelsScreen() }
        composable(Routes.VIEW_ALL_PLAYERS_STATS) { ViewAllPlayersStatsScreen() }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
    }
}
