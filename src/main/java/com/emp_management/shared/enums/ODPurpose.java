package com.emp_management.shared.enums;
/**
 * Why the employee is on duty.
 * This replaces the old LeaveType.ON_DUTY enum — OD is not a leave type;
 * it is a separate work-mode tracked here.
 */
public enum ODPurpose {
    CLIENT_VISIT,       // Visiting a client site
    FIELD_WORK,         // On-site / field operations
    TRAINING,           // External training / conference
    GOVERNMENT_WORK,    // Government office visits, filings, etc.
    BUSINESS_TRAVEL,    // Any official travel not covered above
    OTHER               // Catch-all; reason field is mandatory for this value
}