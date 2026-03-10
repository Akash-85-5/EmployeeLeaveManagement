package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {

    // salary history
    List<EmployeeSalary> findByEmployeeId(Long employeeId);

    // salary used for payroll
    @Query("""
        SELECT e FROM EmployeeSalary e
        WHERE e.employeeId = :employeeId
        AND e.effectiveFrom <= :date
        ORDER BY e.effectiveFrom DESC
    """)
    List<EmployeeSalary> findEffectiveSalary(Long employeeId, LocalDate date);

}