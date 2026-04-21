package com.emp_management.shared.enums;

public enum SeparationStatus {

    // ── Approval stages ───────────────────────────────────────────
    PENDING_MANAGER,        // Waiting for manager to approve
    PENDING_HR,             // Waiting for HR to approve
    PENDING_CEO,            // Waiting for CEO to approve
    REJECTED,               // Rejected at any stage

    // ── After approval ────────────────────────────────────────────
    APPROVED,               // Approved — Admin will start notice period

    // ── Notice period lifecycle ───────────────────────────────────
    NOTICE_PERIOD,          // Employee is actively serving notice period
    NOTICE_COMPLETED,       // 3 months done — Admin fills exit checklist

    // ── Exit process ──────────────────────────────────────────────
    EXIT_CHECKLIST_DONE,    // Laptop / ID card / KT all confirmed by Admin

    // ── Final ─────────────────────────────────────────────────────
    RELIEVED                // CFO generated payslip — process complete
}
