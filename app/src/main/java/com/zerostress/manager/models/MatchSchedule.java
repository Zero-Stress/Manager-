package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MatchSchedule implements Serializable {
    @DocumentId
    private String id;
    private String title;
    private String description;
    private long scheduledTime;
    private String mapName;
    private String gameMode;
    private String status; // "scheduled", "ongoing", "completed", "cancelled"
    private List<String> confirmedPlayers;
    private String createdBy;
    private long createdAt;

    public MatchSchedule() {
        confirmedPlayers = new ArrayList<>();
    }

    public MatchSchedule(String title, long scheduledTime, String mapName, String createdBy) {
        this.title = title;
        this.scheduledTime = scheduledTime;
        this.mapName = mapName;
        this.createdBy = createdBy;
        this.status = "scheduled";
        this.confirmedPlayers = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getConfirmedPlayers() { return confirmedPlayers; }
    public void setConfirmedPlayers(List<String> confirmedPlayers) { this.confirmedPlayers = confirmedPlayers; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getConfirmedCount() {
        return confirmedPlayers != null ? confirmedPlayers.size() : 0;
    }
}
