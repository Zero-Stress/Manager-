package com.zerostress.manager.models;

import java.io.Serializable;

/**
 * Stores all customizable app theme settings.
 * Saved in Firestore at: appSettings/theme
 */
public class AppCustomizer implements Serializable {

    // ========== COLORS ==========
    private String primaryColor = "#38bdf8";        // Main accent (buttons, headers)
    private String secondaryColor = "#10b981";       // Secondary accent (success, online)
    private String backgroundColor = "#090d16";      // Main background
    private String cardColor = "#0f1729";            // Card backgrounds
    private String inputColor = "#1e3a5f";           // Input field backgrounds
    private String navBarColor = "#131c31";          // Bottom navigation bar
    private String textColor = "#f1f5f9";            // Primary text
    private String secondaryTextColor = "#94a3b8";   // Secondary/muted text
    private String dangerColor = "#ef4444";          // Delete, error, leave
    private String warningColor = "#f59e0b";         // Warnings, pending
    private String borderColor = "#1e3a5f";          // Dividers, borders

    // ========== TEXT ==========
    private String appName = "Zero Stress";
    private String welcomeMessage = "Welcome to Zero Stress!";
    private String fontFamily = "sans-serif";        // sans-serif, serif, monospace
    private float headerTextSize = 20f;
    private float bodyTextSize = 14f;

    // ========== FEATURES ==========
    private boolean showOnlineStatus = true;
    private boolean showAchievements = true;
    private boolean showRewardPoints = true;
    private boolean showAnalytics = true;
    private boolean showSquads = true;
    private boolean showTournaments = true;
    private boolean showSchedule = true;
    private boolean showAttendance = true;
    private boolean showChat = true;
    private boolean showVoiceChat = true;

    // ========== LAYOUT ==========
    private boolean gradientHeader = true;
    private boolean roundedCards = true;
    private int cardCornerRadius = 16;
    private int cardPadding = 16;
    private boolean darkMode = true;

    // ========== UI SIZE ==========
    private int buttonHeight = 40;           // Button height in dp
    private int buttonCornerRadius = 12;      // Button corner radius in dp
    private float buttonTextSize = 14f;       // Button text size in sp
    private int iconSize = 24;                // Icon size in dp
    private float headerTextSize = 20f;       // Header text size in sp
    private float bodyTextSize = 14f;         // Body text size in sp
    private int inputHeight = 44;             // Input field height in dp
    private int navBarHeight = 56;            // Bottom nav bar height in dp

    // Default constructor
    public AppCustomizer() {}

    // Getters and Setters
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String c) { this.primaryColor = c; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String c) { this.secondaryColor = c; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String c) { this.backgroundColor = c; }

    public String getCardColor() { return cardColor; }
    public void setCardColor(String c) { this.cardColor = c; }

    public String getInputColor() { return inputColor; }
    public void setInputColor(String c) { this.inputColor = c; }

    public String getNavBarColor() { return navBarColor; }
    public void setNavBarColor(String c) { this.navBarColor = c; }

    public String getTextColor() { return textColor; }
    public void setTextColor(String c) { this.textColor = c; }

    public String getSecondaryTextColor() { return secondaryTextColor; }
    public void setSecondaryTextColor(String c) { this.secondaryTextColor = c; }

    public String getDangerColor() { return dangerColor; }
    public void setDangerColor(String c) { this.dangerColor = c; }

    public String getWarningColor() { return warningColor; }
    public void setWarningColor(String c) { this.warningColor = c; }

    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String c) { this.borderColor = c; }

    public String getAppName() { return appName; }
    public void setAppName(String s) { this.appName = s; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String s) { this.welcomeMessage = s; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String f) { this.fontFamily = f; }

    public float getHeaderTextSize() { return headerTextSize; }
    public void setHeaderTextSize(float s) { this.headerTextSize = s; }

    public float getBodyTextSize() { return bodyTextSize; }
    public void setBodyTextSize(float s) { this.bodyTextSize = s; }

    public boolean isShowOnlineStatus() { return showOnlineStatus; }
    public void setShowOnlineStatus(boolean b) { this.showOnlineStatus = b; }

    public boolean isShowAchievements() { return showAchievements; }
    public void setShowAchievements(boolean b) { this.showAchievements = b; }

    public boolean isShowRewardPoints() { return showRewardPoints; }
    public void setShowRewardPoints(boolean b) { this.showRewardPoints = b; }

    public boolean isShowAnalytics() { return showAnalytics; }
    public void setShowAnalytics(boolean b) { this.showAnalytics = b; }

    public boolean isShowSquads() { return showSquads; }
    public void setShowSquads(boolean b) { this.showSquads = b; }

    public boolean isShowTournaments() { return showTournaments; }
    public void setShowTournaments(boolean b) { this.showTournaments = b; }

    public boolean isShowSchedule() { return showSchedule; }
    public void setShowSchedule(boolean b) { this.showSchedule = b; }

    public boolean isShowAttendance() { return showAttendance; }
    public void setShowAttendance(boolean b) { this.showAttendance = b; }

    public boolean isShowChat() { return showChat; }
    public void setShowChat(boolean b) { this.showChat = b; }

    public boolean isShowVoiceChat() { return showVoiceChat; }
    public void setShowVoiceChat(boolean b) { this.showVoiceChat = b; }

    public boolean isGradientHeader() { return gradientHeader; }
    public void setGradientHeader(boolean b) { this.gradientHeader = b; }

    public boolean isRoundedCards() { return roundedCards; }
    public void setRoundedCards(boolean b) { this.roundedCards = b; }

    public int getCardCornerRadius() { return cardCornerRadius; }
    public void setCardCornerRadius(int r) { this.cardCornerRadius = r; }

    public int getCardPadding() { return cardPadding; }
    public void setCardPadding(int p) { this.cardPadding = p; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean b) { this.darkMode = b; }

    // ========== UI SIZE GETTERS/SETTERS ==========
    public int getButtonHeight() { return buttonHeight; }
    public void setButtonHeight(int v) { this.buttonHeight = v; }

    public int getButtonCornerRadius() { return buttonCornerRadius; }
    public void setButtonCornerRadius(int v) { this.buttonCornerRadius = v; }

    public float getButtonTextSize() { return buttonTextSize; }
    public void setButtonTextSize(float v) { this.buttonTextSize = v; }

    public int getIconSize() { return iconSize; }
    public void setIconSize(int v) { this.iconSize = v; }

    public int getInputHeight() { return inputHeight; }
    public void setInputHeight(int v) { this.inputHeight = v; }

    public int getNavBarHeight() { return navBarHeight; }
    public void setNavBarHeight(int v) { this.navBarHeight = v; }

    // ========== PRESET THEMES ==========

    public static AppCustomizer getPreset(String name) {
        AppCustomizer c = new AppCustomizer();
        switch (name) {
            case "ocean":
                c.setPrimaryColor("#38bdf8");
                c.setSecondaryColor("#10b981");
                c.setBackgroundColor("#090d16");
                c.setCardColor("#0f1729");
                break;
            case "neon":
                c.setPrimaryColor("#a855f7");
                c.setSecondaryColor("#ec4899");
                c.setBackgroundColor("#0f0a1a");
                c.setCardColor("#1a1030");
                c.setInputColor("#2d1b4e");
                break;
            case "fire":
                c.setPrimaryColor("#ef4444");
                c.setSecondaryColor("#f59e0b");
                c.setBackgroundColor("#1a0a0a");
                c.setCardColor("#2a1010");
                c.setInputColor("#3d1a1a");
                c.setDangerColor("#ff6b6b");
                break;
            case "forest":
                c.setPrimaryColor("#22c55e");
                c.setSecondaryColor("#3b82f6");
                c.setBackgroundColor("#0a1a0f");
                c.setCardColor("#0f2a15");
                c.setInputColor("#1a3d20");
                break;
            case "gold":
                c.setPrimaryColor("#eab308");
                c.setSecondaryColor("#f97316");
                c.setBackgroundColor("#1a1508");
                c.setCardColor("#2a2510");
                c.setInputColor("#3d3518");
                c.setWarningColor("#fbbf24");
                break;
            case "midnight":
                c.setPrimaryColor("#6366f1");
                c.setSecondaryColor("#8b5cf6");
                c.setBackgroundColor("#0a0a1a");
                c.setCardColor("#12122a");
                c.setInputColor("#1e1e3d");
                c.setNavBarColor("#151530");
                break;
            default:
                break;
        }
        return c;
    }
}
