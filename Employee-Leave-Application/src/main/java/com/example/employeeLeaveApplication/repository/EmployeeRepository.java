package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findEmployeesByManagerId(Long managerId);

//    @Query("SELECT u FROM User u WHERE u.role = 'EMPLOYEE'")
//    List<Employee> findAllEmployees();
}
