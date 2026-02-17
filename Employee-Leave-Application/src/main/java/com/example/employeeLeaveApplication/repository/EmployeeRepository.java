package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findEmployeesByManagerId(Long managerId);
    List<Employee> findByRole(Role role);

    // Search employees by name (case-insensitive, partial match)
    List<Employee> findByNameContainingIgnoreCase(String name);

    // Count active/inactive employees
    Long countByActive(Boolean active);


//    @Query("SELECT u FROM User u WHERE u.role = 'EMPLOYEE'")
//    List<Employee> findAllEmployees();
}
