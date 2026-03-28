package com.example.antyspamer;

public class AlertItem {
    private int id;
    private String keyword;
    private String context;
    private String timestamp;
    private String status;

    public AlertItem(int id, String keyword, String context, String timestamp, String status) {
        this.id = id;
        this.keyword = keyword;
        this.context = context;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getContext() { return context; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}
