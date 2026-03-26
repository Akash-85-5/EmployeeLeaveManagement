package com.example.employeeLeaveApplication.feature.announcement.dto;

public class AnnouncementRequest {

    private String title;
    private String message;

    private Long teamId;         // optional, only for specific team
    private boolean isGlobal;    // true = all employees
    private String replacementName; // optional

    // ✅ Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public boolean isGlobal() { return isGlobal; }
    public void setGlobal(boolean global) { isGlobal = global; }

    public String getReplacementName() { return replacementName; }
    public void setReplacementName(String replacementName) { this.replacementName = replacementName; }
}