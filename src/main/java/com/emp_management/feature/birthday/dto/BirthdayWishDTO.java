package com.emp_management.feature.birthday.dto;

import java.time.LocalDateTime;

public class BirthdayWishDTO {

    private Long id;
    private String birthdayEmployeeId;
    private String wishedByEmployeeId; // employeeCode string like "WENXT035"
    private String wishedByName;
    private String wishedByRole;
    private String wishMessage;
    private int wishYear;
    private boolean isSystemWish;
    private LocalDateTime createdAt;

    public BirthdayWishDTO() {}

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

    public int getWishYear() { return wishYear; }
    public void setWishYear(int wishYear) { this.wishYear = wishYear; }

    public boolean isSystemWish() { return isSystemWish; }
    public void setSystemWish(boolean systemWish) { isSystemWish = systemWish; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
