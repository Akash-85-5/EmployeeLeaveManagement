package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.shared.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmployee_Email(String email);

    List<User> findByEmployee_Role(Role role);

    Optional<User> findByEmployee_Id(String employeeId);

    // For login by employeeId string like "WENXT01"
    Optional<User> findByEmployee_EmployeeId(String employeeId);
}
