package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    @DocumentId
    private String id;
    private String senderName;
    private String senderPhone;
    private String message;
    private long timestamp;
    private String type; // "text", "announcement", "system"

    public ChatMessage() {}

    public ChatMessage(String senderName, String senderPhone, String message) {
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.type = "text";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
