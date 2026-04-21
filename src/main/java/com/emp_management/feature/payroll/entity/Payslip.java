package com.emp_management.feature.payroll.entity;

import com.emp_management.shared.converter.AESBigDecimalConverter;
import com.emp_management.shared.enums.PayrollStatus;
import com.emp_management.shared.enums.TaxRegime;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payslip",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id","year","month"}))
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;

    private Integer month;

    private Integer year;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal basicSalary;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal hra;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal conveyance;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal medical;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal otherAllowance;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal bonus;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal incentive;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal stipend;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal grossSalary;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal pf;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal esi;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal professionalTax;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal tds;

    private Double lopDays;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal lop;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal netSalary;

    private LocalDate generatedDate;

    @Enumerated(EnumType.STRING)
    private PayrollStatus status;

    @Convert(converter = AESBigDecimalConverter.class)
    private BigDecimal variablePay;

    // In Payslip.java — add this field

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime")
    private TaxRegime taxRegime = TaxRegime.OLD;

    // getter + setter
    public TaxRegime getTaxRegime() { return taxRegime; }
    public void setTaxRegime(TaxRegime taxRegime) { this.taxRegime = taxRegime; }

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }

    public BigDecimal getHra() {
        return hra;
    }

    public void setHra(BigDecimal hra) {
        this.hra = hra;
    }

    public BigDecimal getConveyance() {
        return conveyance;
    }

    public void setConveyance(BigDecimal conveyance) {
        this.conveyance = conveyance;
    }

    public BigDecimal getMedical() {
        return medical;
    }

    public void setMedical(BigDecimal medical) {
        this.medical = medical;
    }

    public BigDecimal getOtherAllowance() {
        return otherAllowance;
    }

    public void setOtherAllowance(BigDecimal otherAllowance) {
        this.otherAllowance = otherAllowance;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public BigDecimal getIncentive() {
        return incentive;
    }

    public void setIncentive(BigDecimal incentive) {
        this.incentive = incentive;
    }

    public BigDecimal getStipend() {
        return stipend;
    }

    public void setStipend(BigDecimal stipend) {
        this.stipend = stipend;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }

    public BigDecimal getPf() {
        return pf;
    }

    public void setPf(BigDecimal pf) {
        this.pf = pf;
    }

    public BigDecimal getEsi() {
        return esi;
    }

    public void setEsi(BigDecimal esi) {
        this.esi = esi;
    }

    public BigDecimal getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(BigDecimal professionalTax) {
        this.professionalTax = professionalTax;
    }

    public BigDecimal getTds() {
        return tds;
    }

    public void setTds(BigDecimal tds) {
        this.tds = tds;
    }

    public BigDecimal getLop() {
        return lop;
    }

    public void setLop(BigDecimal lop) {
        this.lop = lop;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public PayrollStatus getStatus() {
        return status;
    }

    public void setStatus(PayrollStatus status) {
        this.status = status;
    }

    public Double getLopDays() {
        return lopDays;
    }

    public void setLopDays(Double lopDays) {
        this.lopDays = lopDays;
    }

    public BigDecimal getVariablePay() {
        return variablePay;
    }

    public void setVariablePay(BigDecimal variablePay) {
        this.variablePay = variablePay;
    }

}