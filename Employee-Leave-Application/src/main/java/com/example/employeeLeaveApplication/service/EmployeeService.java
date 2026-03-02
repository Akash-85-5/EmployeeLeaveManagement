package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.ProfileResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.User;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.enums.Status;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeService(EmployeeRepository employeeRepository,UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository=userRepository;
    }

    // ==================== EXISTING METHODS ====================

//    public Employee createEmployee(Employee employee) {
//        return employeeRepository.save(employee);
//    }
//    public Employee getEmployee(Long id) {
//        return employeeRepository.findById(id)
//                .orElseThrow(() -> new BadRequestException("Employee not found"));
//    }


    public ProfileResponse getProfile(Long employeeId) {

        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProfileResponse response = new ProfileResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setManagerId(user.getManagerId());

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee Not found"));

        if (employee.getManagerId() != null) {
            Employee manager = employeeRepository.findById(employee.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager Not Found"));

            response.setManagerName(manager.getName());
        } else {
            response.setManagerName(null);
        }
        response.setActive(user.getStatus() == Status.ACTIVE);
        response.setMustChangePassword(user.isForcePwdChange());

        response.setJoiningDate(user.getJoiningDate());
        response.setBiometricStatus(user.getBiometricStatus().name());
        response.setVpnStatus(user.getVpnStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
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