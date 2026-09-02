package com.zerostress.manager.models;

public class ChatMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;
    private boolean deleted;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.deleted = false;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    public boolean isDeleted() { return deleted; }

    public void setId(String id) { this.id = id; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
