package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;
import java.io.Serializable;

public class MatchRecord implements Serializable {
    @DocumentId
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

    @DocumentId
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getMatches() { return matches; }
    public void setMatches(int matches) { this.matches = matches; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getAvgDamage() { return avgDamage; }
    public void setAvgDamage(int avgDamage) { this.avgDamage = avgDamage; }
    public int getSurvivalSeconds() { return survivalSeconds; }
    public void setSurvivalSeconds(int survivalSeconds) { this.survivalSeconds = survivalSeconds; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
