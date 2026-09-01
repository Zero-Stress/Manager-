package com.zerostress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostress.data.model.*
import com.zerostress.ui.screens.admin.*
import com.zerostress.ui.screens.player.*
import com.zerostress.ui.theme.*
import com.zerostress.viewmodel.AppViewModel
import com.zerostress.viewmodel.VoiceChatViewModel

// Player nav items (bottom bar)
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Leaderboard : Screen("leaderboard", "Ranks", Icons.Default.EmojiEvents)
    data object Social : Screen("social", "Social", Icons.Default.People)
    data object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

// Extra features (accessed via "More" grid)
sealed class ExtraFeature(val label: String, val icon: ImageVector, val color: Color) {
    data object SmartAI : ExtraFeature("AI Tools", Icons.Default.Psychology, Color(0xFF00BCD4))
    data object Missions : ExtraFeature("Missions", Icons.Default.EmojiEvents, ZSGreen)
    data object Economy : ExtraFeature("Economy", Icons.Default.AttachMoney, ZSOrange)
    data object MiniGames : ExtraFeature("Games", Icons.Default.SportsEsports, Color(0xFF9C27B0))
    data object Analytics : ExtraFeature("Analytics", Icons.Default.Analytics, ZSBlue)
    data object VoiceChat : ExtraFeature("Voice", Icons.Default.RecordVoiceOver, Color(0xFF4CAF50))
    data object EnhancedProfile : ExtraFeature("Profile+", Icons.Default.Star, Color(0xFFFF9800))
    data object AdminTools : ExtraFeature("Admin", Icons.Default.Build, ZSRed)
}

// Admin bottom bar items
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
    var showExtraMenu by remember { mutableStateOf(false) }
    var selectedExtra by remember { mutableStateOf<ExtraFeature?>(null) }
    val isAdmin = currentUser.isAdmin

    // Collect all state
    val players by viewModel.players.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val channels by voiceViewModel.channels.collectAsState()
    val currentChannel by voiceViewModel.currentChannel.collectAsState()
    val isMuted by voiceViewModel.isMuted.collectAsState()
    val isDeafened by voiceViewModel.isDeafened.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val seasonPass by viewModel.seasonPass.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val shopItems by viewModel.shopItems.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val trainingSessions by viewModel.trainingSessions.collectAsState()
    val scrimmages by viewModel.scrimmages.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val hallOfFame by viewModel.hallOfFame.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val playerTitles by viewModel.playerTitles.collectAsState()

    // If an extra feature is selected, show it full screen
    if (selectedExtra != null) {
        Scaffold(topBar = {
            TopAppBar(title = { Text(selectedExtra!!.label) },
                navigationIcon = { IconButton(onClick = { selectedExtra = null }) { Icon(Icons.Default.ArrowBack, "Back") } })
        }) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedExtra) {
                    ExtraFeature.SmartAI -> SmartFeaturesScreen(
                        players = players, leaderboard = leaderboard, dailyLogs = dailyLogs,
                        insights = insights, hallOfFame = hallOfFame,
                        onBuildTeam = { /* auto build */ },
                        onGenerateInsights = { viewModel.generateInsights(dailyLogs) },
                        onGenerateHallOfFame = { viewModel.getHallOfFame(dailyLogs) }
                    )
                    ExtraFeature.Missions -> MissionsChallengesScreen(
                        missions = missions, seasonPass = seasonPass,
                        onClaimMission = viewModel::claimMission,
                        onClaimSeasonReward = viewModel::claimSeasonReward,
                        onCreateMission = viewModel::createMission,
                        isAdmin = isAdmin
                    )
                    ExtraFeature.Economy -> EconomyScreen(
                        coinBalance = coinBalance, shopItems = shopItems,
                        playerName = currentUser.name,
                        onSpinWheel = { viewModel.dailySpin(currentUser.name) },
                        onPurchase = { viewModel.purchaseShopItem(currentUser.name, it) },
                        onBuyLootBox = { viewModel.addCoins(currentUser.name, it) }
                    )
                    ExtraFeature.MiniGames -> MiniGamesScreen(
                        playerName = currentUser.name, onAddCoins = viewModel::addCoins
                    )
                    ExtraFeature.Analytics -> AnalyticsScreen(
                        playerName = currentUser.name, dailyLogs = dailyLogs,
                        leaderboard = leaderboard, insights = insights,
                        onGenerateInsights = { viewModel.generateInsights(dailyLogs) }
                    )
                    ExtraFeature.VoiceChat -> VoiceChatScreen(
                        channels = channels, currentChannel = currentChannel,
                        currentUser = currentUser, isMuted = isMuted, isDeafened = isDeafened,
                        onCreateChannel = { name, type -> voiceViewModel.createChannel(name, type, currentUser.name) },
                        onJoinChannel = { voiceViewModel.joinChannel(it, currentUser) },
                        onLeaveChannel = { voiceViewModel.leaveChannel() },
                        onToggleMute = { voiceViewModel.toggleMute() },
                        onToggleDeafen = { voiceViewModel.toggleDeafen() },
                        onDeleteChannel = { voiceViewModel.deleteChannel(it) },
                        onKickParticipant = { chId, phone -> voiceViewModel.kickParticipant(chId, phone) }
                    )
                    ExtraFeature.EnhancedProfile -> ProfileEnhancedScreen(
                        player = currentUser, dailyLogs = dailyLogs, leaderboard = leaderboard,
                        coinBalance = coinBalance, titles = playerTitles, achievements = achievements,
                        capsules = emptyList(), onLogout = onLogout
                    )
                    ExtraFeature.AdminTools -> AdminToolsScreen(
                        trainingSessions = trainingSessions, scrimmages = scrimmages,
                        expenses = expenses, players = players, dailyLogs = dailyLogs,
                        onCreateTraining = viewModel::createTraining,
                        onCreateScrimmage = viewModel::createScrimmage,
                        onAddExpense = viewModel::addExpense,
                        onDeleteExpense = { /* delete */ },
                        currentAdminName = currentUser.name
                    )
                    else -> {}
                }
            }
        }
        return
    }

    // Main navigation
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                if (isAdmin) {
                    val adminItems = listOf(AdminScreen.Dashboard, AdminScreen.DailyInput, AdminScreen.Announcements)
                    adminItems.forEachIndexed { index, screen ->
                        NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                            icon = { Icon(screen.icon, null) }, label = { Text(screen.label, fontSize = 10.sp) })
                    }
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Screen.Leaderboard.icon, null) }, label = { Text("Ranks", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                        icon = { Icon(Screen.Social.icon, null) }, label = { Text("Social", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 5, onClick = { selectedTab = 5 },
                        icon = { Icon(Screen.Chat.icon, null) }, label = { Text("Chat", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 6, onClick = { selectedTab = 6 },
                        icon = { Icon(Screen.Profile.icon, null) }, label = { Text("Profile", fontSize = 10.sp) })
                    NavigationBarItem(selected = selectedTab == 7, onClick = { selectedTab = 7 },
                        icon = { Icon(Icons.Default.Menu, null) }, label = { Text("More", fontSize = 10.sp) })
                } else {
                    val playerItems = listOf(Screen.Leaderboard, Screen.Social, Screen.Chat, Screen.Profile)
                    playerItems.forEachIndexed { index, screen ->
                        NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                            icon = { Icon(screen.icon, null) }, label = { Text(screen.label, fontSize = 10.sp) })
                    }
                    NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Menu, null) }, label = { Text("More", fontSize = 10.sp) })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> AdminDashboard(players = players, onApprove = viewModel::approvePlayer,
                        onReject = viewModel::rejectPlayer, onDelete = viewModel::deletePlayer,
                        onAddPlayer = { name, phone, pw -> viewModel.addPlayer(name, phone, pw) },
                        onResetPassword = { phone, pw -> viewModel.resetPassword(phone, pw) },
                        onRoleChange = { phone, role -> viewModel.updatePlayerRole(phone, role) })
                    1 -> DailyInputScreen(players = players, onSave = viewModel::addDailyRecord)
                    2 -> AnnouncementsScreen(announcements = announcements,
                        onPost = { viewModel.postAnnouncement(it, currentUser.name) },
                        onDelete = viewModel::deleteAnnouncement, isAdmin = true)
                    3 -> LeaderboardScreen(leaderboard = leaderboard)
                    4 -> SocialScreen(posts = feedPosts, stories = stories, players = players,
                        currentUserName = currentUser.name, onCreatePost = viewModel::createPost,
                        onLikePost = viewModel::likePost, onComment = viewModel::commentOnPost,
                        onPostStory = viewModel::postStory, onDeletePost = viewModel::deletePost, isAdmin = true)
                    5 -> ChatScreen(messages = chatMessages, currentUserName = currentUser.name, isAdmin = true,
                        onSend = { viewModel.sendMessage(currentUser.name, it, true) },
                        onReply = { msg, sender -> viewModel.replyToMessage(currentUser.name, msg, sender, true) },
                        onDelete = { viewModel.deleteMessage(it) }, onClearChat = { viewModel.clearChat() })
                    6 -> ProfileEnhancedScreen(player = currentUser, dailyLogs = dailyLogs, leaderboard = leaderboard,
                        coinBalance = coinBalance, titles = playerTitles, achievements = achievements,
                        capsules = emptyList(), onLogout = onLogout)
                    7 -> MoreMenuGrid(isAdmin = true) { selectedExtra = it }
                }
            } else {
                when (selectedTab) {
                    0 -> LeaderboardScreen(leaderboard = leaderboard)
                    1 -> SocialScreen(posts = feedPosts, stories = stories, players = players,
                        currentUserName = currentUser.name, onCreatePost = viewModel::createPost,
                        onLikePost = viewModel::likePost, onComment = viewModel::commentOnPost,
                        onPostStory = viewModel::postStory, onDeletePost = { }, isAdmin = false)
                    2 -> ChatScreen(messages = chatMessages, currentUserName = currentUser.name, isAdmin = false,
                        onSend = { viewModel.sendMessage(currentUser.name, it, false) },
                        onReply = { msg, sender -> viewModel.replyToMessage(currentUser.name, msg, sender, false) },
                        onDelete = { }, onClearChat = { })
                    3 -> ProfileEnhancedScreen(player = currentUser, dailyLogs = dailyLogs, leaderboard = leaderboard,
                        coinBalance = coinBalance, titles = playerTitles, achievements = achievements,
                        capsules = emptyList(), onLogout = onLogout)
                    4 -> MoreMenuGrid(isAdmin = false) { selectedExtra = it }
                }
            }
        }
    }
}

@Composable
fun MoreMenuGrid(isAdmin: Boolean, onSelect: (ExtraFeature) -> Unit) {
    val features = buildList {
        add(ExtraFeature.SmartAI)
        add(ExtraFeature.Missions)
        add(ExtraFeature.Economy)
        add(ExtraFeature.MiniGames)
        add(ExtraFeature.Analytics)
        add(ExtraFeature.VoiceChat)
        add(ExtraFeature.EnhancedProfile)
        if (isAdmin) add(ExtraFeature.AdminTools)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("⚡ More Features", fontWeight = FontWeight.Bold, fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(features) { feature ->
                Card(shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onSelect(feature) },
                    colors = CardDefaults.cardColors(containerColor = feature.color.copy(alpha = 0.15f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(feature.label, fontWeight = FontWeight.Medium, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
