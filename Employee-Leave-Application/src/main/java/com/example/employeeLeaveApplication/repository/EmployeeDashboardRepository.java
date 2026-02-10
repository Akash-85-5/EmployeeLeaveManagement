package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeDashboardRepository extends JpaRepository<Employee, Long>{
}
