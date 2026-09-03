package com.zerostress.manager.models;

public class VoiceUser {
    public enum Status {
        ONLINE,
        IDLE,
        DO_NOT_DISTURB
    }

    public enum VoiceState {
        CONNECTED,
        MUTED,
        DEAFENED,
        SCREEN_SHARING,
        HAND_RAISED,
        SPEAKING
    }

    private String userId;
    private String userName;
    private String userStatus;
    private String voiceState;
    private boolean isMuted;
    private boolean isDeafened;
    private boolean isScreenSharing;
    private boolean isHandRaised;
    private boolean isSpeaking;
    private boolean isHost;
    private boolean isSpeaker; // For stage channels
    private long joinedAt;
    private long lastActive;

    public VoiceUser() {}

    public VoiceUser(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.userStatus = Status.ONLINE.name();
        this.voiceState = VoiceState.CONNECTED.name();
        this.isMuted = false;
        this.isDeafened = false;
        this.isScreenSharing = false;
        this.isHandRaised = false;
        this.isSpeaking = false;
        this.isHost = false;
        this.isSpeaker = false;
        this.joinedAt = System.currentTimeMillis();
        this.lastActive = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String userStatus) { this.userStatus = userStatus; }

    public String getVoiceState() { return voiceState; }
    public void setVoiceState(String voiceState) { this.voiceState = voiceState; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    public boolean isDeafened() { return isDeafened; }
    public void setDeafened(boolean deafened) { isDeafened = deafened; }

    public boolean isScreenSharing() { return isScreenSharing; }
    public void setScreenSharing(boolean screenSharing) { isScreenSharing = screenSharing; }

    public boolean isHandRaised() { return isHandRaised; }
    public void setHandRaised(boolean handRaised) { isHandRaised = handRaised; }

    public boolean isSpeaking() { return isSpeaking; }
    public void setSpeaking(boolean speaking) { isSpeaking = speaking; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    public boolean isSpeaker() { return isSpeaker; }
    public void setSpeaker(boolean speaker) { isSpeaker = speaker; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public long getLastActive() { return lastActive; }
    public void setLastActive(long lastActive) { this.lastActive = lastActive; }
}
