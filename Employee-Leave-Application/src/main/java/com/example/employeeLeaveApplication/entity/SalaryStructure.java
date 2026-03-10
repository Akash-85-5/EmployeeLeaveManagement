package com.example.employeeLeaveApplication.entity;

import com.example.employeeLeaveApplication.enums.Role;
import jakarta.persistence.*;

@Entity
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;

    private double hraAmount;

    private double transportAllowance;

    private double pfPercent;

    private double taxPercent;

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