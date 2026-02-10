package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email); // ✅ Added for EmployeeService check

    List<Employee> findByManagerId(Long managerId);

    Optional<Employee> findByRole(Role role);
}
