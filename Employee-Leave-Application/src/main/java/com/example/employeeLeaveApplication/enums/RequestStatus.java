package com.example.employeeLeaveApplication.enums;

public enum RequestStatus {
    PENDING_TL,        // Initial state for regular employees
    PENDING_MANAGER,   // Initial state for TLs OR second stage for employees
    PENDING_HR,        // Initial state for Managers OR final stage for others
    APPROVED,          // Final state after HR signs off
    REJECTED           // If any level denies the request
}
