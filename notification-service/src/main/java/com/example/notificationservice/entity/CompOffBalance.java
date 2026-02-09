package com.example.notificationservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comp_off_balance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year"}))
public class CompOffBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private Integer year;

    private Double earned = 0.0;
    private Double used = 0.0;
    private Double balance = 0.0;

    private LocalDateTime updatedAt;

    public void calculateBalance() {
        this.balance = earned - used;
        if (balance < 0) balance = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getEarned() {
        return earned;
    }

    public void setEarned(Double earned) {
        this.earned = earned;
    }

    public Double getUsed() {
        return used;
    }

    public void setUsed(Double used) {
        this.used = used;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

