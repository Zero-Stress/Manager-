package com.zs.admin.models;

public class MatchRecord {
    private String id;
    private String playerName;
    private int matches;
    private int wins;
    private int kills;
    private int assists;
    private int damage;
    private int avgDamage;
    private int survivalSeconds;
    private long timestamp;

    public MatchRecord() {}

    public MatchRecord(String playerName, int matches, int wins, int kills, int assists,
                       int damage, int avgDamage, int survivalSeconds, long timestamp) {
        this.playerName = playerName;
        this.matches = matches;
        this.wins = wins;
        this.kills = kills;
        this.assists = assists;
        this.damage = damage;
        this.avgDamage = avgDamage;
        this.survivalSeconds = survivalSeconds;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public int getMatches() { return matches; }
    public int getWins() { return wins; }
    public int getKills() { return kills; }
    public int getAssists() { return assists; }
    public int getDamage() { return damage; }
    public int getAvgDamage() { return avgDamage; }
    public int getSurvivalSeconds() { return survivalSeconds; }
    public long getTimestamp() { return timestamp; }
}
