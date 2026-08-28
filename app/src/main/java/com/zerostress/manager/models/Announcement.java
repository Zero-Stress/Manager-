package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

public class Announcement {
    @DocumentId
    private String id;
    private String message;
    private long timestamp;

    public Announcement() {}

    public Announcement(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    @DocumentId
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
