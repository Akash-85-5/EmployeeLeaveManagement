package com.example.employeeLeaveApplication.enums;

public enum AccessRequestStatus {
    DRAFT("Draft - not yet submitted"),
    SUBMITTED("Waiting for manager approval"),
    MANAGER_APPROVED("Manager approved"),
    MANAGER_REJECTED("Manager rejected"),
    ADMIN_APPROVED("Admin approved - access granted"),
    ADMIN_REJECTED("Admin rejected");

    private final String description;

    AccessRequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}