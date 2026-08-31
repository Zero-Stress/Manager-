package com.zs.admin.models;

import java.util.List;

public class Player {
    private String phone;
    private String name;
    private String password;
    private String role;
    private String status;
    private boolean online;
    private long lastSeen;
    private String playerRole;
    private long rewardPoints;
    private long totalRewardPoints;
    private boolean voiceChatAllowed;
    private List<String> squadMembers;

    public Player() {}

    public Player(String phone, String name, String password, String role, String status) {
        this.phone = phone;
        this.name = name;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public String getPhone() { return phone; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public boolean isCurrentlyOnline() { return online || (System.currentTimeMillis() - lastSeen < 60000); }
    public long getLastSeen() { return lastSeen; }
    public String getPlayerRole() { return playerRole; }
    public long getRewardPoints() { return rewardPoints; }
    public long getTotalRewardPoints() { return totalRewardPoints; }
    public boolean isVoiceChatAllowed() { return voiceChatAllowed; }
}
