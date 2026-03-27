package com.example.employeeLeaveApplication.shared.enums;

/**
 * TerminationGrounds
 *
 * These are ELIGIBILITY CONDITIONS — they tell the system
 * WHY a termination WITHOUT notice period is allowed.
 *
 * This is NOT the reason for termination. The actual reason
 * (e.g. "Found sharing client data") is a free-text `reason`
 * field typed by the manager.
 *
 *   PROBATION   → Employee is in their probation period
 *   INTERNSHIP  → Employee is an intern or in training
 *   MISCONDUCT  → Employee broke company rules or law
 *                 (harassment, data leak, fraud, etc.)
 */
public enum TerminationGrounds {
    PROBATION,
    INTERNSHIP,
    MISCONDUCT
}