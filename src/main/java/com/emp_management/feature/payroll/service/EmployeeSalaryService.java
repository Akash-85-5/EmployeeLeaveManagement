//package com.emp_management.feature.payroll.service;
//
//import com.emp_management.feature.payroll.entity.EmployeeSalary;
//import com.emp_management.feature.payroll.repository.EmployeeSalaryRepository;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class EmployeeSalaryService {
//
//    private final EmployeeSalaryRepository repository;
//
//    public EmployeeSalaryService(EmployeeSalaryRepository repository) {
//        this.repository = repository;
//    }
//
//    // Assign salary (first time or increment)
//    public EmployeeSalary assignSalary(EmployeeSalary salary) {
//
//        if (salary.getEmployeeId() == null) {
//            throw new RuntimeException("Employee ID is required");
//        }
//
//        if (salary.getBasicSalary() == null) {
//            throw new RuntimeException("Basic salary is required");
//        }
//
//        if (salary.getBasicSalary().compareTo(BigDecimal.ZERO) < 0) {
//            throw new RuntimeException("Basic salary cannot be negative");
//        }
//
//        // If effective date not provided → today
//        if (salary.getEffectiveFrom() == null) {
//            salary.setEffectiveFrom(LocalDate.now());
//        }
//
//        Optional<EmployeeSalary> existing =
//                repository.findByEmployee_EmpIdAndEffectiveFrom(
//                        salary.getEmployeeId(),
//                        salary.getEffectiveFrom()
//                );
//
//        if (existing.isPresent()) {
//
//            EmployeeSalary existingSalary = existing.get();
//
//            existingSalary.setBasicSalary(salary.getBasicSalary());
//
//            return repository.save(existingSalary);
//        }
//
//        return repository.save(salary);
//    }
//
//    // Salary history
//    public List<EmployeeSalary> getSalaryHistory(Long employeeId) {
//
//        if (employeeId == null) {
//            throw new RuntimeException("Employee ID is required");
//        }
//
//        return repository.findByEmployee_EmpIdOrderByEffectiveFromDesc(employeeId);
//    }
//
//    // Current salary
//    public EmployeeSalary getCurrentSalary(Long employeeId) {
//
//        if (employeeId == null) {
//            throw new RuntimeException("Employee ID is required");
//        }
//
//        return repository
//                .findEffectiveSalary(employeeId, LocalDate.now())
//                .stream()
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Salary not found"));
//    }
//}
