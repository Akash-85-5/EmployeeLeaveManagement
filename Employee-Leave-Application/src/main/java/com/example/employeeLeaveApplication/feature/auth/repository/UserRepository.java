package com.example.employeeLeaveApplication.feature.auth.repository;

import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployee_Email(String email);
    List<User> findByEmployee_Role(Role role);
    Optional<User> findByEmployee_Id(Long employeeId);
}
