package com.emp_management.feature.birthday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendWishRequest {

    @NotNull(message = "birthdayEmployeeId is required")
    private String birthdayEmployeeId;

    @NotBlank(message = "wishedByEmployeeId is required")
    private String wishedByEmployeeId; // "WENXT035" employeeCode

    @NotBlank(message = "wishMessage is required")
    @Size(max = 1000, message = "wishMessage must be at most 1000 characters")
    private String wishMessage;

    public String getBirthdayEmployeeId() { return birthdayEmployeeId; }
    public void setBirthdayEmployeeId(String birthdayEmployeeId) { this.birthdayEmployeeId = birthdayEmployeeId; }

    public String getWishedByEmployeeId() { return wishedByEmployeeId; }
    public void setWishedByEmployeeId(String wishedByEmployeeId) { this.wishedByEmployeeId = wishedByEmployeeId; }

    public String getWishMessage() { return wishMessage; }
    public void setWishMessage(String wishMessage) { this.wishMessage = wishMessage; }
}
