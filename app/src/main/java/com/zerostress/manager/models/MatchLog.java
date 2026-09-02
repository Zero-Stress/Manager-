package com.zerostress.manager.models;

public class MatchLog {
    private String id;
    private String playerId;
    private String playerName;
    private int kills;
    private int deaths;
    private int assists;
    private long damage;
    private boolean win;
    private String matchType;
    private long date;

    public MatchLog() {}

    public MatchLog(String playerId, String playerName, int kills, int deaths, int assists, long damage, boolean win) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.damage = damage;
        this.win = win;
        this.date = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public long getDamage() { return damage; }
    public boolean isWin() { return win; }
    public String getMatchType() { return matchType; }
    public long getDate() { return date; }

    public void setId(String id) { this.id = id; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setKills(int kills) { this.kills = kills; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void setAssists(int assists) { this.assists = assists; }
    public void setDamage(long damage) { this.damage = damage; }
    public void setWin(boolean win) { this.win = win; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public void setDate(long date) { this.date = date; }

    public long getScore() {
        return (long)(kills * 10 + damage / 100 + (win ? 200 : 0));
    }
}
