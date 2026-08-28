package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Squad implements Serializable {
    @DocumentId
    private String id;
    private String name;
    private String color;
    private List<String> memberPhones;
    private long createdAt;
    private int totalWins;
    private int totalKills;
    private int totalMatches;

    public Squad() {
        memberPhones = new ArrayList<>();
    }

    public Squad(String name, String color) {
        this.name = name;
        this.color = color;
        this.memberPhones = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.totalWins = 0;
        this.totalKills = 0;
        this.totalMatches = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getMemberPhones() { return memberPhones; }
    public void setMemberPhones(List<String> memberPhones) { this.memberPhones = memberPhones; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }

    public int getMemberCount() {
        return memberPhones != null ? memberPhones.size() : 0;
    }
}
