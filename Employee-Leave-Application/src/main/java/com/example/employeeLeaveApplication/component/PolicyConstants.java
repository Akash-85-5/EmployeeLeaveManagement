package com.example.employeeLeaveApplication.component;

public final class PolicyConstants {

    // ═══════════════════════════════════════════════════════════════
    // MONTHLY LEAVE POLICIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Maximum leaves allowed per month (for non-COMP_OFF types)
     */
    public static final double MONTHLY_LIMIT = 2.0;

    // ═══════════════════════════════════════════════════════════════
    // CARRY FORWARD POLICIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Eligibility threshold for carry forward
     * Rule: If yearlyBalance ≤ 10, eligible
     * If yearlyBalance > 10, carry forward only 10
     */
    public static final double CARRY_FORWARD_ELIGIBILITY_THRESHOLD = 10.0;

    /**
     * Maximum days that can be carried forward to next year
     */
    public static final double MAX_CARRY_FORWARD = 10.0;

    // ═══════════════════════════════════════════════════════════════
    // LOSS OF PAY POLICIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Loss of pay percentage per excess day
     * Example: 1 excess day = 1% LOP
     */
    public static final double LOSS_OF_PAY_PERCENT_PER_DAY = 1.0;

    // ═══════════════════════════════════════════════════════════════
    // YEARLY LEAVE ALLOCATIONS (Total = 24 days)
    // ═══════════════════════════════════════════════════════════════

    public static final double VACATION_YEARLY_ALLOCATION = 8.0;
    public static final double SICK_YEARLY_ALLOCATION = 6.0;
    public static final double CASUAL_YEARLY_ALLOCATION = 6.0;
    public static final double PERSONAL_YEARLY_ALLOCATION = 4.0;

    /**
     * Total yearly allocation (sum of all leave types)
     * VACATION(8) + SICK(6) + CASUAL(6) + PERSONAL(4) = 24 days
     */
    public static final double TOTAL_YEARLY_ALLOCATION =
            VACATION_YEARLY_ALLOCATION +
                    SICK_YEARLY_ALLOCATION +
                    CASUAL_YEARLY_ALLOCATION +
                    PERSONAL_YEARLY_ALLOCATION; // = 24.0

    // ═══════════════════════════════════════════════════════════════
    // IMPORTANT: CompOff is NOT allocated, it's EARNED
    // CompOff balance comes from working extra hours/holidays
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE CONSTRUCTOR (Prevent instantiation)
    // ═══════════════════════════════════════════════════════════════

    private PolicyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
