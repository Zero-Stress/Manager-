package com.zerostress.manager.models;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private String id;
    private String name;
    private String phone;
    private String role;
    private String status;
    private long score;
    private int kills;
    private int deaths;
    private int assists;
    private long damage;
    private int wins;
    private int matches;
    private int xp;
    private int level;
    private int coins;
    private String rank;
    private String fcmToken;

    public Player() {}

    public Player(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.role = "player";
        this.status = "pending";
        this.score = 0;
        this.kills = 0;
        this.deaths = 0;
        this.assists = 0;
        this.damage = 0;
        this.wins = 0;
        this.matches = 0;
        this.xp = 0;
        this.level = 1;
        this.coins = 0;
        this.rank = "Iron";
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public long getScore() { return score; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public long getDamage() { return damage; }
    public int getWins() { return wins; }
    public int getMatches() { return matches; }
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public int getCoins() { return coins; }
    public String getRank() { return rank; }
    public String getFcmToken() { return fcmToken; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setScore(long score) { this.score = score; }
    public void setKills(int kills) { this.kills = kills; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void setAssists(int assists) { this.assists = assists; }
    public void setDamage(long damage) { this.damage = damage; }
    public void setWins(int wins) { this.wins = wins; }
    public void setMatches(int matches) { this.matches = matches; }
    public void setXp(int xp) { this.xp = xp; }
    public void setLevel(int level) { this.level = level; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setRank(String rank) { this.rank = rank; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", id);
        map.put("name", name);
        map.put("phone", phone);
        map.put("role", role);
        map.put("status", status);
        map.put("score", score);
        map.put("kills", kills);
        map.put("deaths", deaths);
        map.put("assists", assists);
        map.put("damage", damage);
        map.put("wins", wins);
        map.put("matches", matches);
        map.put("xp", xp);
        map.put("level", level);
        map.put("coins", coins);
        map.put("rank", rank);
        return map;
    }

    public static long calculateScore(int kills, long damage, int wins) {
        return (long)(kills * 10 + damage / 100 + wins * 50);
    }

    public static String getRankTier(long score) {
        if (score >= 5000) return "Mythic";
        if (score >= 4000) return "Diamond";
        if (score >= 3000) return "Platinum";
        if (score >= 2000) return "Gold";
        if (score >= 1200) return "Silver";
        if (score >= 600) return "Bronze";
        return "Iron";
    }

    public static int xpForLevel(int level) {
        return level * 500;
    }

    public double getWinRate() {
        return matches > 0 ? (wins * 100.0 / matches) : 0;
    }

    public double getAvgDamage() {
        return matches > 0 ? (damage * 1.0 / matches) : 0;
    }
}
