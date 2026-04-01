package com.emp_management.feature.employee.repository;

import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeOnboardingRepository extends JpaRepository<EmployeeOnboarding,Long> {
    Optional<EmployeeOnboarding> findByEmployee_EmpId(String empId);
}
