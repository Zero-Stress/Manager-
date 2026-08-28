package com.zerostress.manager.models;

import java.io.Serializable;

public class Achievement implements Serializable {
    private String id;
    private String name;
    private String description;
    private String icon;
    private boolean unlocked;
    private long unlockedAt;
    private String category; // "kills", "wins", "matches", "streak", "special"

    public Achievement() {}

    public Achievement(String id, String name, String description, String icon, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.unlocked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public long getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(long unlockedAt) { this.unlockedAt = unlockedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // Static factory for all achievements
    public static Achievement[] getAllAchievements() {
        return new Achievement[]{
            new Achievement("first_blood", "First Blood", "Get your first kill", "🔫", "kills"),
            new Achievement("kill_50", "Killer", "Get 50 total kills", "💀", "kills"),
            new Achievement("kill_100", "Terminator", "Get 100 total kills", "☠️", "kills"),
            new Achievement("kill_500", "Legendary", "Get 500 total kills", "👑", "kills"),
            new Achievement("first_win", "Winner Winner", "Win your first match", "🏆", "wins"),
            new Achievement("win_10", "Champion", "Win 10 matches", "🥇", "wins"),
            new Achievement("win_50", "Dominant", "Win 50 matches", "🏅", "wins"),
            new Achievement("matches_10", "Regular", "Play 10 matches", "🎯", "matches"),
            new Achievement("matches_50", "Veteran", "Play 50 matches", "⭐", "matches"),
            new Achievement("matches_100", "Hardcore", "Play 100 matches", "🔥", "matches"),
            new Achievement("damage_10000", "Heavy Hitter", "Deal 10,000 total damage", "💥", "kills"),
            new Achievement("damage_50000", "Demolisher", "Deal 50,000 total damage", "💣", "kills"),
            new Achievement("mvp_week", "MVP of the Week", "Top scorer this week", "🌟", "special"),
            new Achievement("perfect_game", "Perfect Game", "Win with 10+ kills in a match", "💎", "special"),
            new Achievement("team_player", "Team Player", "Get 20+ assists total", "🤝", "special"),
        };
    }

    public static boolean checkAchievement(Achievement a, int kills, int wins, int matches, int damage, int assists) {
        switch (a.getId()) {
            case "first_blood": return kills >= 1;
            case "kill_50": return kills >= 50;
            case "kill_100": return kills >= 100;
            case "kill_500": return kills >= 500;
            case "first_win": return wins >= 1;
            case "win_10": return wins >= 10;
            case "win_50": return wins >= 50;
            case "matches_10": return matches >= 10;
            case "matches_50": return matches >= 50;
            case "matches_100": return matches >= 100;
            case "damage_10000": return damage >= 10000;
            case "damage_50000": return damage >= 50000;
            case "team_player": return assists >= 20;
            default: return false;
        }
    }
}
