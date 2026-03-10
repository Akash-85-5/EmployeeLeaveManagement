package com.example.employeeLeaveApplication.entity;

import jakarta.persistence.*;

@Entity
public class PayrollSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double hraAmount;

    private double transportAllowance;

    private double pfPercent;

    private double taxPercent;

    public Long getId() {
        return id;
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