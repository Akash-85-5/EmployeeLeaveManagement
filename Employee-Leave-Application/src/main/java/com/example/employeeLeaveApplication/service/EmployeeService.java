package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ==================== EXISTING METHODS ====================

//    public Employee createEmployee(Employee employee) {
//        return employeeRepository.save(employee);
//    }

    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
    }

    // ==================== NEW METHODS ====================

    /**
     * Get all employees with filters and pagination
     */
    public Page<Employee> getAllEmployees(
            String name,
            String email,
            String role,
            Long managerId,
            Boolean active,
            Pageable pageable
    ) {
        return employeeRepository.findAll(
                createSpecification(name, email, role, managerId, active),
                pageable
        );
    }

    /**
     * Update employee
     */
    @Transactional
    public Employee updateEmployee(Long id, Employee employee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        if (employee.getName() != null) {
            existing.setName(employee.getName());
        }
        if (employee.getEmail() != null) {
            existing.setEmail(employee.getEmail());
        }
        if (employee.getRole() != null) {
            existing.setRole(employee.getRole());
        }
        if (employee.getManagerId() != null) {
            existing.setManagerId(employee.getManagerId());
        }


        return employeeRepository.save(existing);
    }

    /**
     * Delete/Deactivate employee
     */
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    /**
     * Get team members for a manager
     */
    public List<Employee> getTeamMembers(Long managerId) {
        return employeeRepository.findByManagerId(managerId);
    }

    /**
     * Search employees by name
     */
    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByNameContainingIgnoreCase(query);
    }

    /**
     * Get active employees count
     */
    public Long getActiveEmployeesCount() {
        return employeeRepository.countByActive(true);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create JPA Specification for filtering
     */
    private Specification<Employee> createSpecification(
            String name,
            String email,
            String role,
            Long managerId,
            Boolean active
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (role != null && !role.isEmpty()) {
                predicates.add(cb.equal(root.get("role"), Role.valueOf(role.toUpperCase())));
            }
            if (managerId != null) {
                predicates.add(cb.equal(root.get("managerId"), managerId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}