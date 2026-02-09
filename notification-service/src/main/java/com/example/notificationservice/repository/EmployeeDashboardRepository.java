package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeDashboardRepository extends JpaRepository<Employee, Long>{
}
