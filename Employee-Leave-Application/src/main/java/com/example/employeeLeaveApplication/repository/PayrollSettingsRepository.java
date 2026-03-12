package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.PayrollSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollSettingsRepository extends JpaRepository<PayrollSettings, Long> {
}