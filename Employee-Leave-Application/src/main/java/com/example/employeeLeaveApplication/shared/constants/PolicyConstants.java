package com.example.employeeLeaveApplication.shared.constants;

public final class PolicyConstants {

    // ═══════════════════════════════════════════════════════════════
    // ANNUAL LEAVE — CUMULATIVE MONTHLY POLICY
    // ═══════════════════════════════════════════════════════════════

    /**
     * ANNUAL_LEAVE accrues at 1.5 days per month (18 / 12).
     * Each month the employee gets 2 usable days from the accrual.
     *
     * Cumulative rule:
     *   - Jan: 2 days available (accrued 1.5, but usable cap = 2)
     *   - Feb (if Jan unused): 4 days available (2 + 2)
     *   - ... rolls up until December
     *   - Unused at year-end → carry forward (max 10 days)
     *
     * We track cumulative available days in AnnualLeaveMonthlyBalance.
     */
    public static final double ANNUAL_LEAVE_PER_MONTH          = 2.0;   // usable days added each month
    public static final double ANNUAL_LEAVE_YEARLY_ALLOCATION  = 18.0;  // total allocation per year
    public static final double SICK_YEARLY_ALLOCATION          = 12.0;  // flat, no monthly accrual
    public static final int    MATERNITY_DAYS                  = 90;    // female only
    public static final int    PATERNITY_DAYS                  = 5;     // male only

    // ═══════════════════════════════════════════════════════════════
    // CARRY FORWARD POLICIES  (ANNUAL_LEAVE only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * If year-end ANNUAL_LEAVE remaining <= 10  → carry all of it forward.
     * If year-end ANNUAL_LEAVE remaining >  10  → carry only 10 days.
     * SICK, COMP_OFF, MATERNITY, PATERNITY are NOT carried forward.
     */
    public static final double MAX_CARRY_FORWARD = 10.0;

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    private PolicyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}