package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.SalaryStructure;
import com.example.employeeLeaveApplication.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    Optional<SalaryStructure> findByRole(Role role);

    boolean existsByRole(Role role);
}