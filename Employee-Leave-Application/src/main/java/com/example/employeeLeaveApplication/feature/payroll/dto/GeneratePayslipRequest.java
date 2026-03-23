package com.example.employeeLeaveApplication.feature.payroll.dto;

import lombok.Data;

@Data
public class GeneratePayslipRequest {

    private Long employeeId;

    private Integer year;

    private Integer month;

    private double basicSalary;

    private double hra;

    private double transportAllowance;

    private double pfPercent;

    private double taxPercent;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public double getHra() {
        return hra;
    }

    public void setHra(double hra) {
        this.hra = hra;
    }

    public double getTransportAllowance() {
        return transportAllowance;
    }

    public void setTransportAllowance(double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public double getPfPercent() {
        return pfPercent;
    }

    public void setPfPercent(double pfPercent) {
        this.pfPercent = pfPercent;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }
}