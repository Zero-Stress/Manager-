package com.zerostress.manager.models;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String fromUserName;
    private String toUserId;
    private String status; // pending, accepted, rejected
    private long timestamp;

    public FriendRequest() {}

    public FriendRequest(String fromUserId, String fromUserName, String toUserId) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getFromUserId() { return fromUserId; }
    public String getFromUserName() { return fromUserName; }
    public String getToUserId() { return toUserId; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
}
