package com.emp_management.feature.employee.repository;

import com.emp_management.feature.employee.entity.EmployeeSeparation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeSeparationRepository extends JpaRepository <EmployeeSeparation, Long> {
}
