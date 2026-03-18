package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
}
