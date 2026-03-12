package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.EmployeeSalary;
import com.example.employeeLeaveApplication.repository.EmployeeSalaryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

        if (salary.getEffectiveFrom() == null) {
            salary.setEffectiveFrom(LocalDate.now());
        }

        return repository.save(salary);
    }

    // Salary history
    public List<EmployeeSalary> getSalaryHistory(Long employeeId) {

        return repository.findByEmployeeId(employeeId);
    }

    // Current salary
    public EmployeeSalary getCurrentSalary(Long employeeId) {

        return repository
                .findEffectiveSalary(employeeId, LocalDate.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Salary not found"));
    }
}