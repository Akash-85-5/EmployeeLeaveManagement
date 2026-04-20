package com.emp_management.feature.payroll.dto;

import java.math.BigDecimal;

public class EmployeePayslipStatusResponse {

    private String employeeId;
    private String employeeName;
    private String designation;

    // "GENERATED", "DRAFT", "NOT_CREATED"
    private String payslipStatus;

    // Only populated if payslip exists (DRAFT or GENERATED)
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;

    // Add to EmployeePayslipStatusResponse.java

    private String taxRegime; // "OLD" or "NEW" — from employee default

    public String getTaxRegime() { return taxRegime; }
    public void setTaxRegime(String taxRegime) { this.taxRegime = taxRegime; }

    // Getters & Setters

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPayslipStatus() { return payslipStatus; }
    public void setPayslipStatus(String payslipStatus) { this.payslipStatus = payslipStatus; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
}