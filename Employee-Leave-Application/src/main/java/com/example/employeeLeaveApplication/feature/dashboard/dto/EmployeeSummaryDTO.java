package com.example.employeeLeaveApplication.feature.dashboard.dto;

import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;

public class EmployeeSummaryDTO {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long reportingId;

    public EmployeeSummaryDTO() {}

    // ✅ Static factory method — easy to call from DashboardService
    public static EmployeeSummaryDTO from(Employee e) {
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.id = e.getId();
        dto.name = e.getName();
        dto.email = e.getEmail();
        dto.role = e.getRole();
        dto.reportingId = e.getReportingId();
        return dto;
    }

    public Long getReportingId() {
        return reportingId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}