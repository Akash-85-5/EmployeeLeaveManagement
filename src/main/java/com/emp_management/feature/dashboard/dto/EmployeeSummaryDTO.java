package com.emp_management.feature.dashboard.dto;


import com.emp_management.feature.employee.entity.Employee;

public class EmployeeSummaryDTO {

    private String id;
    private String name;
    private String email;
    private String role;
    private String reportingId;


    public EmployeeSummaryDTO() {
    }

    public static EmployeeSummaryDTO from(Employee e) {
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.id = e.getEmpId();
        dto.name = e.getName();
        dto.email = e.getEmail();
        dto.role = e.getRole().getRoleName();
        dto.reportingId = e.getReportingId();
        return dto;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getReportingId() {
        return reportingId;
    }
}