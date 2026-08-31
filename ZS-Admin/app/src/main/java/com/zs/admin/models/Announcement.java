package com.zs.admin.models;

public class Announcement {
    private String id;
    private String message;
    private long timestamp;

    public Announcement() {}

    public Announcement(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
