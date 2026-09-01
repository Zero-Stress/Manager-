package com.zerostress.manager.models

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val category: String = "combat", // combat, social, consistency, milestone
    val requirement: Int = 0,
    val type: String = "kills" // kills, wins, matches, damage, streak, friends, chats
)

data class PlayerAchievement(
    val phone: String = "",
    val achievementId: String = "",
    val unlockedAt: Long = System.currentTimeMillis()
)

val ALL_ACHIEVEMENTS = listOf(
    // Combat
    Achievement("first_blood", "First Blood", "Get your first kill", "🔫", "combat", 1, "kills"),
    Achievement("sharpshooter", "Sharpshooter", "Get 50 total kills", "🎯", "combat", 50, "kills"),
    Achievement("rampage", "Rampage", "Get 200 total kills", "💀", "combat", 200, "kills"),
    Achievement("legend", "Legend", "Get 500 total kills", "⚔️", "combat", 500, "kills"),
    Achievement("godlike", "Godlike", "Get 1000 total kills", "👹", "combat", 1000, "kills"),
    Achievement("damage_dealer", "Damage Dealer", "Deal 10,000 total damage", "💥", "combat", 10000, "damage"),
    Achievement("damage_master", "Damage Master", "Deal 50,000 total damage", "🔥", "combat", 50000, "damage"),
    // Wins
    Achievement("first_win", "First Win", "Win your first match", "🏆", "milestone", 1, "wins"),
    Achievement("winner", "Winner", "Win 10 matches", "🥇", "milestone", 10, "wins"),
    Achievement("champion", "Champion", "Win 50 matches", "👑", "milestone", 50, "wins"),
    Achievement("unstoppable", "Unstoppable", "Win 100 matches", "🌟", "milestone", 100, "wins"),
    // Matches
    Achievement("rookie", "Rookie", "Play 5 matches", "📋", "consistency", 5, "matches"),
    Achievement("regular", "Regular", "Play 25 matches", "📅", "consistency", 25, "matches"),
    Achievement("veteran", "Veteran", "Play 100 matches", "🎖️", "consistency", 100, "matches"),
    Achievement("hardcore", "Hardcore", "Play 500 matches", "💪", "consistency", 500, "matches"),
    // Social
    Achievement("friendly", "Friendly", "Add your first friend", "🤝", "social", 1, "friends"),
    Achievement("popular", "Popular", "Have 10 friends", "❤️", "social", 10, "friends"),
    Achievement("social_butterfly", "Social Butterfly", "Send 50 chat messages", "💬", "social", 50, "chats"),
    // Streak
    Achievement("on_fire", "On Fire!", "3-day login streak", "🔥", "consistency", 3, "streak"),
    Achievement("unbreakable", "Unbreakable", "7-day login streak", "⚡", "consistency", 7, "streak"),
    Achievement("dedicated", "Dedicated", "30-day login streak", "💎", "consistency", 30, "streak")
)
