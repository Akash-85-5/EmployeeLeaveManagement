package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.entity.Employee;

public class EmployeeSummaryDTO {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long managerId;

    public EmployeeSummaryDTO() {}

    // ✅ Static factory method — easy to call from DashboardService
    public static EmployeeSummaryDTO from(Employee e) {
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.id = e.getId();
        dto.name = e.getName();
        dto.email = e.getEmail();
        dto.role = e.getRole();
        dto.managerId = e.getManagerId();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public Long getManagerId() { return managerId; }
}