package com.emp_management.feature.employee.repository;

import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeOnboardingRepository extends JpaRepository<EmployeeOnboarding,Long> {
}
