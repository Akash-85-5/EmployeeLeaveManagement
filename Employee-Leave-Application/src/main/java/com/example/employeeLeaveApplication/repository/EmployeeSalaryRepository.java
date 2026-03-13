package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {

    List<EmployeeSalary> findByEmployeeId(Long employeeId);

    @Query("""
        SELECT e FROM EmployeeSalary e
        WHERE e.employeeId = :employeeId
        AND e.effectiveFrom <= :date
        ORDER BY e.effectiveFrom DESC
    """)
    List<EmployeeSalary> findEffectiveSalary(Long employeeId, LocalDate date);

    Optional<EmployeeSalary> findByEmployeeIdAndEffectiveFrom(
            Long employeeId,
            LocalDate effectiveFrom
    );
}