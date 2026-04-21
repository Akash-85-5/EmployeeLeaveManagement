package com.emp_management.feature.leave.carryforward.entity;

import com.emp_management.feature.employee.entity.Employee;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "carry_forward_balance")
public class CarryForwardBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "carry_year", nullable = false)
    private Integer year;

    @Column(name = "total_carried_forward", nullable = false)
    private Double totalCarriedForward = 0.0;

    @Column(name = "total_used", nullable = false)
    private Double totalUsed = 0.0;

    @Column(nullable = false)
    private Double remaining = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getTotalCarriedForward() { return totalCarriedForward; }
    public void setTotalCarriedForward(Double totalCarriedForward) { this.totalCarriedForward = totalCarriedForward; }

    public Double getTotalUsed() { return totalUsed; }
    public void setTotalUsed(Double totalUsed) { this.totalUsed = totalUsed; }

    public Double getRemaining() { return remaining; }
    public void setRemaining(Double remaining) { this.remaining = remaining; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}