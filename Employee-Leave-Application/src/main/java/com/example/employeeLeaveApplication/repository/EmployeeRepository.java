package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {


    Optional<Employee> findByEmail(String email);
    List<Employee> findByManagerId(Long managerId);
    List<Employee> findEmployeesByManagerId(Long managerId);


    List<Employee> findByNameContainingIgnoreCase(String name);

    Long countByActive(Boolean active);
}