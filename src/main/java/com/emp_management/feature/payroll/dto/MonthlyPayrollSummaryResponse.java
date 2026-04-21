package com.emp_management.feature.payroll.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlyPayrollSummaryResponse {

    private Integer month;
    private Integer year;

    // ── Totals ───────────────────────────────────────────
    private BigDecimal totalGrossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetSalary;

    // ── Employee counts ──────────────────────────────────
    private Integer totalEmployees;       // all active employees
    private Integer generatedCount;       // payslip status = GENERATED
    private Integer draftCount;           // payslip status = DRAFT
    private Integer notCreatedCount;      // no payslip at all this month

    // ── Per-employee status list (for the table) ─────────
    private List<EmployeePayslipStatusResponse> employeeStatuses;

    // ── Getters & Setters ────────────────────────────────

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getTotalGrossSalary() { return totalGrossSalary; }
    public void setTotalGrossSalary(BigDecimal totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getTotalNetSalary() { return totalNetSalary; }
    public void setTotalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }

    public Integer getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(Integer totalEmployees) { this.totalEmployees = totalEmployees; }

    public Integer getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Integer generatedCount) { this.generatedCount = generatedCount; }

    public Integer getDraftCount() { return draftCount; }
    public void setDraftCount(Integer draftCount) { this.draftCount = draftCount; }

    public Integer getNotCreatedCount() { return notCreatedCount; }
    public void setNotCreatedCount(Integer notCreatedCount) { this.notCreatedCount = notCreatedCount; }

    public List<EmployeePayslipStatusResponse> getEmployeeStatuses() { return employeeStatuses; }
    public void setEmployeeStatuses(List<EmployeePayslipStatusResponse> employeeStatuses) { this.employeeStatuses = employeeStatuses; }
}