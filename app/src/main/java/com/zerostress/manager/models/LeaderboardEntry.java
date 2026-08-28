package com.zerostress.manager.models;

public class LeaderboardEntry {
    private String playerName;
    private int matches;
    private int wins;
    private int kills;
    private int assists;
    private int damage;
    private int avgDamage;
    private double winRate;
    private double avgKills;
    private int scorePoints;
    private int rank;
    private boolean isOnline;

    public LeaderboardEntry() {}

    // Getters and Setters
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
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    public double getAvgKills() { return avgKills; }
    public void setAvgKills(double avgKills) { this.avgKills = avgKills; }
    public int getScorePoints() { return scorePoints; }
    public void setScorePoints(int scorePoints) { this.scorePoints = scorePoints; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
