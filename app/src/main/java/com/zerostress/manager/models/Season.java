package com.zerostress.manager.models;

public class Season {
    private String id;
    private String name;
    private long startDate;
    private long endDate;
    private boolean active;
    private int topRewardCoins;

    public Season() {}

    public Season(String name, long startDate, long endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
        this.topRewardCoins = 500;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public boolean isActive() { return active; }
    public int getTopRewardCoins() { return topRewardCoins; }

    public void setId(String id) { this.id = id; }
    public void setActive(boolean active) { this.active = active; }
}
