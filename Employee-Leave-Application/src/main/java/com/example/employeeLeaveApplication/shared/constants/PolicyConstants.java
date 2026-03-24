package com.example.employeeLeaveApplication.shared.constants;

public final class PolicyConstants {

    // ═══════════════════════════════════════════════════════════════
    // ANNUAL_LEAVE — CUMULATIVE MONTHLY ACCRUAL
    // ═══════════════════════════════════════════════════════════════

    /**
     * ANNUAL_LEAVE accrues at 1.5 days per month (18 / 12).
     * Unused days roll forward to next month cumulatively.
     * Year-end unused balance → carry forward (max 10 days).
     */
    public static final double ANNUAL_LEAVE_PER_MONTH         = 1.5;
    public static final double ANNUAL_LEAVE_YEARLY_ALLOCATION = 18.0;

    // ═══════════════════════════════════════════════════════════════
    // SICK LEAVE — CUMULATIVE MONTHLY ACCRUAL (resets every year)
    // ═══════════════════════════════════════════════════════════════

    /**
     * SICK accrues at 1.0 day per month (12 / 12).
     * Unused days roll forward to next month cumulatively within the same year.
     * At year-end: SICK balance RESETS — NOT carried forward.
     */
    public static final double SICK_LEAVE_PER_MONTH         = 1.0;
    public static final double SICK_LEAVE_YEARLY_ALLOCATION = 12.0;

    // ═══════════════════════════════════════════════════════════════
    // CARRY FORWARD (ANNUAL_LEAVE only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Max ANNUAL_LEAVE days carried forward to next year.
     * If remaining <= 10 → carry all. If remaining > 10 → carry only 10.
     * SICK, COMP_OFF, MATERNITY, PATERNITY are NOT carried forward.
     */
    public static final double MAX_CARRY_FORWARD = 10.0;

    // ═══════════════════════════════════════════════════════════════
    // MATERNITY / PATERNITY
    // ═══════════════════════════════════════════════════════════════

    public static final int MATERNITY_DAYS = 90;
    public static final int PATERNITY_DAYS = 5;

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL CHAIN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Maximum approval levels for any leave application.
     * Level 1 = employee's direct manager (mapped as TEAM_LEADER in ApprovalLevel)
     * Level 2 = that manager's manager
     * If Level 1 approver has no manager (managerId = null) → only 1 level needed.
     */
    public static final int MAX_APPROVAL_LEVELS = 2;

    private PolicyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}