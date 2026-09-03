package com.zerostress.manager.models;

public class VoiceChannel {
    private String id;
    private String name;
    private String category;
    private boolean active;
    private int maxUsers;
    private boolean isStage; // Stage channel (speaker/audience mode)
    private boolean allowScreenShare;
    private boolean allowRecording;
    private long createdAt;

    public VoiceChannel() {}

    public VoiceChannel(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.active = true;
        this.maxUsers = 10;
        this.isStage = false;
        this.allowScreenShare = true;
        this.allowRecording = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public boolean isStage() { return stage; }
    public void setStage(boolean stage) { isStage = stage; }

    public boolean isAllowScreenShare() { return allowScreenShare; }
    public void setAllowScreenShare(boolean allowScreenShare) { this.allowScreenShare = allowScreenShare; }

    public boolean isAllowRecording() { return allowRecording; }
    public void setAllowRecording(boolean allowRecording) { this.allowRecording = allowRecording; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
