package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChildDto {

    @NotBlank(message = "Child name is required")
    private String childName;

    @NotNull(message = "Child gender is required")
    private Gender gender;

    @NotNull(message = "Child age is required")
    @Min(value = 0, message = "Child age must be 0 or more")
    private Integer age;

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}