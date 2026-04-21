package com.emp_management.feature.dashboard.dto;

public class TeamMember {

    private String employeeId;    // was Long
    private String employeeName;
    private String designation;
    private String skills;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
}