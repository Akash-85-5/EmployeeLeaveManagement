package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.Role;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_structure", uniqueConstraints = @UniqueConstraint(columnNames = "role"))
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Role role;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal conveyance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal medical;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal otherAllowance;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal pfPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal professionalTax;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal esiPercent;

    public Long getId() { return id; }

    public Role getRole() { return role; }

    public BigDecimal getHra() { return hra; }

    public BigDecimal getConveyance() { return conveyance; }

    public BigDecimal getMedical() { return medical; }

    public BigDecimal getOtherAllowance() { return otherAllowance; }

    public BigDecimal getPfPercent() { return pfPercent; }

    public BigDecimal getProfessionalTax() { return professionalTax; }

    public BigDecimal getEsiPercent() { return esiPercent; }

    public void setId(Long id) { this.id = id; }

    public void setRole(Role role) { this.role = role; }

    public void setHra(BigDecimal hra) { this.hra = hra; }

    public void setConveyance(BigDecimal conveyance) { this.conveyance = conveyance; }

    public void setMedical(BigDecimal medical) { this.medical = medical; }

    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }

    public void setPfPercent(BigDecimal pfPercent) { this.pfPercent = pfPercent; }

    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }

    public void setEsiPercent(BigDecimal esiPercent) { this.esiPercent = esiPercent; }
}