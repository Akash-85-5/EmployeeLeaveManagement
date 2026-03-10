package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaryRepository extends JpaRepository<Salary, Long> {

    Optional<Salary> findByEmployeeId(Long employeeId);
}