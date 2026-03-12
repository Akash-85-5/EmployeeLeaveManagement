package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "salary_structure", uniqueConstraints = @UniqueConstraint(columnNames = "role"))
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Role role;

    @Column(nullable = false)
    private Double hraAmount;

    @Column(nullable = false)
    private Double pfPercent;

    @Column(nullable = false)
    private Double taxPercent;

    @Column(nullable = false)
    private Double transportAllowance;

    public void setHraAmount(Double hraAmount) {
        this.hraAmount = hraAmount;
    }

    public void setPfPercent(Double pfPercent) {
        this.pfPercent = pfPercent;
    }

    public void setTaxPercent(Double taxPercent) {
        this.taxPercent = taxPercent;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public double getHraAmount() {
        return hraAmount;
    }

    public void setHraAmount(double hraAmount) {
        this.hraAmount = hraAmount;
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