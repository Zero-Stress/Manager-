package com.zerostress.manager.models;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private int xpReward;
    private int coinReward;
    private String icon;
    private String requirement; // e.g. "kills:100", "wins:50"

    public Achievement() {}

    public Achievement(String name, String description, int xpReward, int coinReward, String icon, String requirement) {
        this.name = name;
        this.description = description;
        this.xpReward = xpReward;
        this.coinReward = coinReward;
        this.icon = icon;
        this.requirement = requirement;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getXpReward() { return xpReward; }
    public int getCoinReward() { return coinReward; }
    public String getIcon() { return icon; }
    public String getRequirement() { return requirement; }

    public void setId(String id) { this.id = id; }
}
