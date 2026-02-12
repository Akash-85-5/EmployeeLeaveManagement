package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findEmployeesByManagerId(Long managerId);
    List<Employee> findByRole(Role role);


//    @Query("SELECT u FROM User u WHERE u.role = 'EMPLOYEE'")
//    List<Employee> findAllEmployees();
}
