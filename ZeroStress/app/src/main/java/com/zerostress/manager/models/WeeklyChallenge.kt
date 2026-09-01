package com.zerostress.manager.models

data class WeeklyChallenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "kills", // kills, damage, wins, streak, survival
    val target: Int = 0,
    val reward: String = "",
    val rewardCoins: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val isActive: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChallengeProgress(
    val id: String = "",
    val challengeId: String = "",
    val phone: String = "",
    val playerName: String = "",
    val currentValue: Int = 0,
    val completed: Boolean = false,
    val completedAt: Long = 0
)

data class MVPVote(
    val id: String = "",
    val matchId: String = "",
    val voterPhone: String = "",
    val candidatePhone: String = "",
    val candidateName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val votes: Map<String, Int> = emptyMap(),
    val createdBy: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class LFGPost(
    val id: String = "",
    val phone: String = "",
    val playerName: String = "",
    val message: String = "",
    val lookingFor: String = "", // squad, duo, custom match
    val skillLevel: String = "", // beginner, intermediate, pro
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlayerReview(
    val id: String = "",
    val reviewerPhone: String = "",
    val targetPhone: String = "",
    val rating: Int = 0, // 1-5
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminSettings(
    val customScoreFormula: String = "(K * 10) + (D / 100) + (W * 50)",
    val bannedPlayers: List<String> = emptyList(),
    val whitelistMode: Boolean = false
)
