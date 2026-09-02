package com.zerostress.manager.models;

public class Announcement {
    private String id;
    private String text;
    private String author;
    private long timestamp;

    public Announcement() {}

    public Announcement(String text, String author) {
        this.text = text;
        this.author = author;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getAuthor() { return author; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setText(String text) { this.text = text; }
    public void setAuthor(String author) { this.author = author; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
