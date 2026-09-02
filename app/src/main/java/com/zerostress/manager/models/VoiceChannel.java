package com.zerostress.manager.models;

import java.util.ArrayList;
import java.util.List;

public class VoiceChannel {
    private String id;
    private String name;
    private boolean active;
    private int maxParticipants;
    private List<String> participants;
    private long createdAt;

    public VoiceChannel() {
        this.participants = new ArrayList<>();
        this.maxParticipants = 10;
        this.active = false;
    }

    public VoiceChannel(String name) {
        this();
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getMaxParticipants() { return maxParticipants; }
    public List<String> getParticipants() { return participants; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
}
