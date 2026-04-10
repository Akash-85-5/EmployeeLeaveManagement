package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ChildDto {

    @NotBlank(message = "Child name is required")
    private String childName;

    @NotNull(message = "Child gender is required")
    private Gender gender;

    @NotNull(message = "Child DOB is required")
    private LocalDate childDateOfBirth;

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public LocalDate getChildDateOfBirth() {
        return childDateOfBirth;
    }

    public void setChildDateOfBirth(LocalDate childDateOfBirth) {
        this.childDateOfBirth = childDateOfBirth;
    }
}