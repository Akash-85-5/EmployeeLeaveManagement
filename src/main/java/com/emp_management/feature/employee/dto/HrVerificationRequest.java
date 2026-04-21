package com.emp_management.feature.employee.dto;


import com.emp_management.shared.enums.VerificationStatus;

/**
 * HR sends this to verify or reject an employee profile.
 * PUT /api/hr/verify/{employeeId}
 *
 * Verify:  { "status": "VERIFIED" }
 * Reject:  { "status": "REJECTED", "remarks": "Aadhaar is blurry." }
 */
public class HrVerificationRequest {

    private VerificationStatus status;

    private String remarks; // required when REJECTED

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}