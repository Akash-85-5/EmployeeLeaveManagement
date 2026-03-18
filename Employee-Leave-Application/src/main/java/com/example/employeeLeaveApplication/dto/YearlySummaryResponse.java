package com.example.employeeLeaveApplication.dto;

import java.math.BigDecimal;

public class YearlySummaryResponse {

    private Integer year;

    private BigDecimal totalBasic;
    private BigDecimal totalHra;
    private BigDecimal totalConveyance;
    private BigDecimal totalMedical;
    private BigDecimal totalOtherAllowance;

    private BigDecimal totalBonus;
    private BigDecimal totalIncentive;
    private BigDecimal totalStipend;

    private BigDecimal totalPf;
    private BigDecimal totalEsi;
    private BigDecimal totalProfessionalTax;
    private BigDecimal totalTds;
    private BigDecimal totalLop;

    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;

    // getters setters

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getTotalBasic() {
        return totalBasic;
    }

    public void setTotalBasic(BigDecimal totalBasic) {
        this.totalBasic = totalBasic;
    }

    public BigDecimal getTotalHra() {
        return totalHra;
    }

    public void setTotalHra(BigDecimal totalHra) {
        this.totalHra = totalHra;
    }

    public BigDecimal getTotalConveyance() {
        return totalConveyance;
    }

    public void setTotalConveyance(BigDecimal totalConveyance) {
        this.totalConveyance = totalConveyance;
    }

    public BigDecimal getTotalMedical() {
        return totalMedical;
    }

    public void setTotalMedical(BigDecimal totalMedical) {
        this.totalMedical = totalMedical;
    }

    public BigDecimal getTotalOtherAllowance() {
        return totalOtherAllowance;
    }

    public void setTotalOtherAllowance(BigDecimal totalOtherAllowance) {
        this.totalOtherAllowance = totalOtherAllowance;
    }

    public BigDecimal getTotalBonus() {
        return totalBonus;
    }

    public void setTotalBonus(BigDecimal totalBonus) {
        this.totalBonus = totalBonus;
    }

    public BigDecimal getTotalIncentive() {
        return totalIncentive;
    }

    public void setTotalIncentive(BigDecimal totalIncentive) {
        this.totalIncentive = totalIncentive;
    }

    public BigDecimal getTotalStipend() {
        return totalStipend;
    }

    public void setTotalStipend(BigDecimal totalStipend) {
        this.totalStipend = totalStipend;
    }

    public BigDecimal getTotalPf() {
        return totalPf;
    }

    public void setTotalPf(BigDecimal totalPf) {
        this.totalPf = totalPf;
    }

    public BigDecimal getTotalEsi() {
        return totalEsi;
    }

    public void setTotalEsi(BigDecimal totalEsi) {
        this.totalEsi = totalEsi;
    }

    public BigDecimal getTotalProfessionalTax() {
        return totalProfessionalTax;
    }

    public void setTotalProfessionalTax(BigDecimal totalProfessionalTax) {
        this.totalProfessionalTax = totalProfessionalTax;
    }

    public BigDecimal getTotalTds() {
        return totalTds;
    }

    public void setTotalTds(BigDecimal totalTds) {
        this.totalTds = totalTds;
    }

    public BigDecimal getTotalLop() {
        return totalLop;
    }

    public void setTotalLop(BigDecimal totalLop) {
        this.totalLop = totalLop;
    }

    public BigDecimal getTotalGrossSalary() {
        return totalGrossSalary;
    }

    public void setTotalGrossSalary(BigDecimal totalGrossSalary) {
        this.totalGrossSalary = totalGrossSalary;
    }

    public BigDecimal getTotalNetSalary() {
        return totalNetSalary;
    }

    public void setTotalNetSalary(BigDecimal totalNetSalary) {
        this.totalNetSalary = totalNetSalary;
    }
}