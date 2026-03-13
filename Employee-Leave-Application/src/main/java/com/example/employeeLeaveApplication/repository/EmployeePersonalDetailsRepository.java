package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeePersonalDetailsRepository
        extends JpaRepository<EmployeePersonalDetails, Long> {

    Optional<EmployeePersonalDetails> findByEmployeeId(Long employeeId);
}