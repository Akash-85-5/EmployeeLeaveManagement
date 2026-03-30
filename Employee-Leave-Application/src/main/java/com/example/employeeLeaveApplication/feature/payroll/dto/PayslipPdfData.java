package com.example.employeeLeaveApplication.feature.payroll.dto;

import java.math.BigDecimal;

/**
 * Flat DTO passed to the Thymeleaf payslip template.
 * Built entirely inside PayslipPdfService — no entity ever touches the template.
 */
public class PayslipPdfData {

    // ── Employee details ──────────────────────────────────────────
    /** Formatted as "WENXT001", "WENXT042", etc. */
    private String employeeCode;
    private String employeeName;
    private String designation;
    private String joiningDate;       // "03 October 2025"
    private String aadharNumber;
    private String uanNumber;
    private String pfNumber;
    private String accountNumber;
    private String bankName;

    // ── Payslip period ────────────────────────────────────────────
    private String monthYear;         // "Feb 2026"
    private Integer workingDays;
    private Double lopDays;

    // ── Income ────────────────────────────────────────────────────
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal conveyance;
    private BigDecimal medical;
    private BigDecimal otherAllowance;
    private BigDecimal bonus;
    private BigDecimal grossSalary;

    // ── Deductions ────────────────────────────────────────────────
    private BigDecimal pf;
    private BigDecimal tds;
    private BigDecimal professionalTax;
    private BigDecimal variablePay;
    private BigDecimal esi;
    private BigDecimal lop;
    private BigDecimal totalDeduction;

    // ── Net ───────────────────────────────────────────────────────
    private BigDecimal netSalary;
    private String netSalaryInWords;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }

    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getUanNumber() { return uanNumber; }
    public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public Integer getWorkingDays() { return workingDays; }
    public void setWorkingDays(Integer workingDays) { this.workingDays = workingDays; }

    public Double getLopDays() { return lopDays; }
    public void setLopDays(Double lopDays) { this.lopDays = lopDays; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }

    public BigDecimal getConveyance() { return conveyance; }
    public void setConveyance(BigDecimal conveyance) { this.conveyance = conveyance; }

    public BigDecimal getMedical() { return medical; }
    public void setMedical(BigDecimal medical) { this.medical = medical; }

    public BigDecimal getOtherAllowance() { return otherAllowance; }
    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }

    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getPf() { return pf; }
    public void setPf(BigDecimal pf) { this.pf = pf; }

    public BigDecimal getTds() { return tds; }
    public void setTds(BigDecimal tds) { this.tds = tds; }

    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }

    public BigDecimal getVariablePay() { return variablePay; }
    public void setVariablePay(BigDecimal variablePay) { this.variablePay = variablePay; }

    public BigDecimal getEsi() { return esi; }
    public void setEsi(BigDecimal esi) { this.esi = esi; }

    public BigDecimal getLop() { return lop; }
    public void setLop(BigDecimal lop) { this.lop = lop; }

    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public String getNetSalaryInWords() { return netSalaryInWords; }
    public void setNetSalaryInWords(String netSalaryInWords) { this.netSalaryInWords = netSalaryInWords; }
}