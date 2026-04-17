package com.emp_management.feature.birthday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "birthday_wish",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"birthday_employee_id", "wished_by_employee_id", "wish_year"}
        ))
public class BirthdayWish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "birthday_employee_id", nullable = false)
    private String birthdayEmployeeId;


    @Column(name = "wished_by_employee_id", nullable = false)
    private String wishedByEmployeeId;

    @Column(name = "wished_by_name", nullable = false, length = 200)
    private String wishedByName;

    @Column(name = "wished_by_role", length = 50)
    private String wishedByRole;

    @Column(name = "wish_message", nullable = false, columnDefinition = "TEXT")
    private String wishMessage;

    @Column(name = "wish_year", nullable = false)
    private Integer wishYear;

    @Column(name = "is_system_wish", nullable = false)
    private boolean systemWish = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.wishYear == null) {
            this.wishYear = java.time.Year.now().getValue();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBirthdayEmployeeId() { return birthdayEmployeeId; }
    public void setBirthdayEmployeeId(String birthdayEmployeeId) { this.birthdayEmployeeId = birthdayEmployeeId; }

    public String getWishedByEmployeeId() { return wishedByEmployeeId; }
    public void setWishedByEmployeeId(String wishedByEmployeeId) { this.wishedByEmployeeId = wishedByEmployeeId; }

    public String getWishedByName() { return wishedByName; }
    public void setWishedByName(String wishedByName) { this.wishedByName = wishedByName; }

    public String getWishedByRole() { return wishedByRole; }
    public void setWishedByRole(String wishedByRole) { this.wishedByRole = wishedByRole; }

    public String getWishMessage() { return wishMessage; }
    public void setWishMessage(String wishMessage) { this.wishMessage = wishMessage; }

    public Integer getWishYear() { return wishYear; }
    public void setWishYear(Integer wishYear) { this.wishYear = wishYear; }

    public boolean isSystemWish() { return systemWish; }
    public void setSystemWish(boolean systemWish) { this.systemWish = systemWish; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}