package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployee_Email(String email);
    Optional<User> findByEmployee_EmpId(String employeeId);
    Optional<User> findByEmployee (Employee emp);
}
