package com.zerostress.manager.models;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class Player implements Serializable {
    private String phone;
    private String name;
    private String password;
    private String role;
    private String status;
    private String playerRole; // "igl", "fragger", "support", "sniper", "medic"
    private long rewardPoints;
    private long totalRewardPoints;

    @PropertyName("online")
    private boolean isOnline;

    private long lastSeen;
    private String fcmToken;
    private boolean voiceChatAllowed;

    public Player() {}

    public Player(String phone, String name, String password, String role, String status) {
        this.phone = phone;
        this.name = name;
        this.password = password;
        this.role = role;
        this.status = status;
        this.isOnline = false;
        this.lastSeen = 0;
        this.playerRole = "fragger";
        this.rewardPoints = 0;
        this.totalRewardPoints = 0;
        this.voiceChatAllowed = false;
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
    public String getPlayerRole() { return playerRole; }
    public void setPlayerRole(String playerRole) { this.playerRole = playerRole; }
    public long getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(long rewardPoints) { this.rewardPoints = rewardPoints; }
    public long getTotalRewardPoints() { return totalRewardPoints; }
    public void setTotalRewardPoints(long totalRewardPoints) { this.totalRewardPoints = totalRewardPoints; }

    @PropertyName("online")
    public boolean isOnline() { return isOnline; }

    @PropertyName("online")
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public boolean isVoiceChatAllowed() { return voiceChatAllowed; }
    public void setVoiceChatAllowed(boolean voiceChatAllowed) { this.voiceChatAllowed = voiceChatAllowed; }

    public boolean isCurrentlyOnline() {
        if (isOnline) return true;
        return lastSeen > 0 && (System.currentTimeMillis() - lastSeen) < 60000;
    }

    public String getRoleEmoji() {
        if (playerRole == null) return "🔫";
        switch (playerRole) {
            case "igl": return "🧠";
            case "fragger": return "🔫";
            case "support": return "🛡️";
            case "sniper": return "🎯";
            case "medic": return "💊";
            default: return "🔫";
        }
    }

    public String getRoleLabel() {
        if (playerRole == null) return "Fragger";
        switch (playerRole) {
            case "igl": return "IGL";
            case "fragger": return "Fragger";
            case "support": return "Support";
            case "sniper": return "Sniper";
            case "medic": return "Medic";
            default: return "Fragger";
        }
    }
}
