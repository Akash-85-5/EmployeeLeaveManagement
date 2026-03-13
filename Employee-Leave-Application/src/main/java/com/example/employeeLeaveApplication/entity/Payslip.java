package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.PayrollStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "payslip",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "year", "month"}
        )
)
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private Long employeeId;

    private Integer month;

    private Integer year;

    private BigDecimal basicSalary;

    private BigDecimal hra;

    private BigDecimal conveyance;

    private BigDecimal medical;

    private BigDecimal otherAllowance;

    private BigDecimal pf;

    private BigDecimal esi;

    private BigDecimal professionalTax;

    @Column(name = "lop_deduction")
    private BigDecimal lop;

    private BigDecimal netSalary;

    private LocalDate generatedDate;

    @Enumerated(EnumType.STRING)
    private PayrollStatus status;

    public Long getId() { return id; }

    public Long getEmployeeId() { return employeeId; }

    public Integer getMonth() { return month; }

    public Integer getYear() { return year; }

    public BigDecimal getBasicSalary() { return basicSalary; }

    public BigDecimal getHra() { return hra; }

    public BigDecimal getConveyance() { return conveyance; }

    public BigDecimal getMedical() { return medical; }

    public BigDecimal getOtherAllowance() { return otherAllowance; }

    public BigDecimal getPf() { return pf; }

    public BigDecimal getEsi() { return esi; }

    public BigDecimal getProfessionalTax() { return professionalTax; }

    public BigDecimal getLop() { return lop; }

    public BigDecimal getNetSalary() { return netSalary; }

    public LocalDate getGeneratedDate() { return generatedDate; }

    public PayrollStatus getStatus() { return status; }

    public void setId(Long id) { this.id = id; }

    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public void setMonth(Integer month) { this.month = month; }

    public void setYear(Integer year) { this.year = year; }

    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public void setHra(BigDecimal hra) { this.hra = hra; }

    public void setConveyance(BigDecimal conveyance) { this.conveyance = conveyance; }

    public void setMedical(BigDecimal medical) { this.medical = medical; }

    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }

    public void setPf(BigDecimal pf) { this.pf = pf; }

    public void setEsi(BigDecimal esi) { this.esi = esi; }

    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }

    public void setLop(BigDecimal lop) { this.lop = lop; }

    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }

    public void setStatus(PayrollStatus status) { this.status = status; }
}