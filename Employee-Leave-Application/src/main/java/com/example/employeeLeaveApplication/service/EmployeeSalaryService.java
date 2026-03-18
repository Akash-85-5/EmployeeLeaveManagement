package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import com.example.employeeLeaveApplication.repository.EmployeeSalaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeSalaryService {

    private final EmployeeSalaryRepository repository;

    public EmployeeSalaryService(EmployeeSalaryRepository repository) {
        this.repository = repository;
    }

    // Assign salary (first time or increment)
    public EmployeeSalary assignSalary(EmployeeSalary salary) {

        if (salary.getEmployeeId() == null) {
            throw new RuntimeException("Employee ID is required");
        }

        if (salary.getBasicSalary() == null) {
            throw new RuntimeException("Basic salary is required");
        }

        if (salary.getBasicSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Basic salary cannot be negative");
        }

        // If effective date not provided → today
        if (salary.getEffectiveFrom() == null) {
            salary.setEffectiveFrom(LocalDate.now());
        }

        Optional<EmployeeSalary> existing =
                repository.findByEmployeeIdAndEffectiveFrom(
                        salary.getEmployeeId(),
                        salary.getEffectiveFrom()
                );

        if (existing.isPresent()) {

            EmployeeSalary existingSalary = existing.get();

            existingSalary.setBasicSalary(salary.getBasicSalary());

            return repository.save(existingSalary);
        }

        return repository.save(salary);
    }

    // Salary history
    public List<EmployeeSalary> getSalaryHistory(Long employeeId) {

        if (employeeId == null) {
            throw new RuntimeException("Employee ID is required");
        }

        return repository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId);
    }

    // Current salary
    public EmployeeSalary getCurrentSalary(Long employeeId) {

        if (employeeId == null) {
            throw new RuntimeException("Employee ID is required");
        }

        return repository
                .findEffectiveSalary(employeeId, LocalDate.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Salary not found"));
    }
}