package com.example.employeeLeaveApplication.feature.admin.dto;

import com.example.employeeLeaveApplication.shared.enums.Role;

import java.time.LocalDate;

public class CreateUserRequest {

    private String name;
    private String email;
    private Role role;
    private Long reportingId;
    private LocalDate joiningDate;


    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }


    public Long getReportingId() {
        return reportingId;
    }

    public void setReportingId(Long reportingId) {
        this.reportingId = reportingId;
    }
}