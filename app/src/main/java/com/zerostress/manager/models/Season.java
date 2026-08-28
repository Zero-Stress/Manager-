package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class Season implements Serializable {
    @DocumentId
    private String id;
    private int seasonNumber;
    private String name;
    private long startDate;
    private long endDate;
    private boolean active;
    private int rewardPointsPerWin;
    private int rewardPointsPerKill;

    public Season() {}

    public Season(int seasonNumber, String name) {
        this.seasonNumber = seasonNumber;
        this.name = name;
        this.startDate = System.currentTimeMillis();
        this.active = true;
        this.rewardPointsPerWin = 50;
        this.rewardPointsPerKill = 10;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getRewardPointsPerWin() { return rewardPointsPerWin; }
    public void setRewardPointsPerWin(int rewardPointsPerWin) { this.rewardPointsPerWin = rewardPointsPerWin; }
    public int getRewardPointsPerKill() { return rewardPointsPerKill; }
    public void setRewardPointsPerKill(int rewardPointsPerKill) { this.rewardPointsPerKill = rewardPointsPerKill; }
}
