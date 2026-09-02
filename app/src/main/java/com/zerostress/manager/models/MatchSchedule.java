package com.zerostress.manager.models;

public class MatchSchedule {
    private String id;
    private String title;
    private String description;
    private long matchTime;
    private String status; // upcoming, ongoing, completed
    private String createdBy;

    public MatchSchedule() {}

    public MatchSchedule(String title, String description, long matchTime, String createdBy) {
        this.title = title;
        this.description = description;
        this.matchTime = matchTime;
        this.createdBy = createdBy;
        this.status = "upcoming";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getMatchTime() { return matchTime; }
    public String getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }

    public void setId(String id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
}
