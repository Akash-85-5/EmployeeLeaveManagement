package com.example.employeeLeaveApplication.feature.leave.lop.dto;

/**
 * Aggregated LOP summary — one row per employee.
 * Used by HR and CFO summary report endpoints only.
 * Sorted by totalLopDays DESC — highest LOP employees appear first.
 *
 * Mapped from JPQL Object[] projection:
 *   index 0 → employeeId   (Long)
 *   index 1 → employeeName (String)
 *   index 2 → employeeRole (String)
 *   index 3 → totalLopDays (Double)
 */
public class LopSummaryResponse {

    private Long   employeeId;
    private String employeeName;
    private String employeeRole;
    private Double totalLopDays;

    // Static factory — maps Object[] row from JPQL GROUP BY query
    public static LopSummaryResponse from(Object[] row) {
        LopSummaryResponse r = new LopSummaryResponse();
        r.employeeId   = ((Number) row[0]).longValue();
        r.employeeName = (String)  row[1];
        r.employeeRole = (String)  row[2];
        r.totalLopDays = ((Number) row[3]).doubleValue();
        return r;
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(String employeeRole) { this.employeeRole = employeeRole; }

    public Double getTotalLopDays() { return totalLopDays; }
    public void setTotalLopDays(Double totalLopDays) { this.totalLopDays = totalLopDays; }
}