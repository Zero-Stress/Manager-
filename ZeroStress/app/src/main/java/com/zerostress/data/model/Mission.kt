package com.zerostress.data.model

data class Mission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "daily", // daily, weekly, boss, reverse, scavenger
    val targetValue: Int = 0,
    val currentValue: Int = 0,
    val rewardCoins: Int = 0,
    val rewardXP: Int = 0,
    val rewardBadge: String = "",
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val expiresAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f
}

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "🏆",
    val category: String = "kills", // kills, wins, streak, social, special
    val requirement: Int = 0,
    val rarity: String = "common", // common, rare, epic, legendary
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

data class SeasonPass(
    val id: String = "",
    val seasonNumber: Int = 1,
    val seasonName: String = "Season 1",
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,
    val isPremium: Boolean = false,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val rewards: List<SeasonReward> = emptyList()
)

data class SeasonReward(
    val level: Int = 0,
    val freeReward: String = "",
    val premiumReward: String = "",
    val isClaimed: Boolean = false
)

data class ZSCoinBalance(
    val playerName: String = "",
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    val lastDailySpin: Long = 0L,
    val loginStreak: Int = 0,
    val lastLoginDate: String = ""
)

data class ShopItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val category: String = "theme", // theme, badge, title, border, avatar
    val icon: String = "",
    val isOwned: Boolean = false,
    val isActive: Boolean = true
)

data class LootBox(
    val id: String = "",
    val name: String = "",
    val cost: Int = 0,
    val rarity: String = "common",
    val rewards: List<String> = emptyList()
)

data class Squad(
    val id: String = "",
    val name: String = "",
    val tag: String = "",
    val captain: String = "",
    val members: List<String> = emptyList(),
    val totalScore: Int = 0,
    val wins: Int = 0,
    val matches: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val winRate: Double
        get() = if (matches > 0) (wins.toDouble() / matches) * 100 else 0.0
}

data class TeamFeedPost(
    val id: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val type: String = "text", // text, highlight, announcement
    val likes: List<String> = emptyList(),
    val comments: List<TeamComment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class TeamComment(
    val id: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TeamStory(
    val id: String = "",
    val authorName: String = "",
    val content: String = "",
    val type: String = "text", // text, match, celebration
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

data class TrainingSession(
    val id: String = "",
    val title: String = "",
    val date: Long = 0L,
    val duration: Int = 60,
    val attendees: List<String> = emptyList(),
    val allPlayers: List<String> = emptyList(),
    val notes: String = "",
    val createdBy: String = ""
)

data class Scrimmage(
    val id: String = "",
    val opponentTeam: String = "",
    val scheduledTime: Long = 0L,
    val status: String = "pending", // pending, confirmed, completed, cancelled
    val ourScore: Int = 0,
    val theirScore: Int = 0,
    val ourPlayers: List<String> = emptyList(),
    val createdBy: String = ""
)

data class ExpenseEntry(
    val id: String = "",
    val description: String = "",
    val amount: Int = 0,
    val category: String = "other", // entry_fee, prize, equipment, other
    val date: Long = System.currentTimeMillis(),
    val recordedBy: String = ""
)

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info", // info, warning, achievement, match, squad
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val actionRoute: String = ""
)

data class Prediction(
    val id: String = "",
    val playerName: String = "",
    val matchId: String = "",
    val predictedKills: Int = 0,
    val actualKills: Int = 0,
    val predictedWins: Int = 0,
    val actualWins: Int = 0,
    val coinsWon: Int = 0,
    val isResolved: Boolean = false
)

data class TimeCapsule(
    val id: String = "",
    val authorName: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val unlockAt: Long = 0L,
    val isUnlocked: Boolean = false
)

data class PlayerTitle(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val requirement: String = "",
    val isUnlocked: Boolean = false
)

data class DailyLoginReward(
    val day: Int = 1,
    val coins: Int = 10,
    val badge: String = "",
    val isClaimed: Boolean = false
)
