package com.zerostress.manager.models;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class Player implements Serializable {
    private String phone;
    private String name;
    private String password;
    private String role;
    private String status;

    @PropertyName("online")
    private boolean isOnline;

    private long lastSeen;
    private String fcmToken;

    public Player() {}

    public Player(String phone, String name, String password, String role, String status) {
        this.phone = phone;
        this.name = name;
        this.password = password;
        this.role = role;
        this.status = status;
        this.isOnline = false;
        this.lastSeen = 0;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @PropertyName("online")
    public boolean isOnline() { return isOnline; }

    @PropertyName("online")
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isCurrentlyOnline() {
        if (isOnline) return true;
        return lastSeen > 0 && (System.currentTimeMillis() - lastSeen) < 60000;
    }
}
