package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeePersonalDetailsRepository
        extends JpaRepository<EmployeePersonalDetails, Long> {

    Optional<EmployeePersonalDetails> findByEmployeeId(Long employeeId);

    // HR uses these to see their verification queue
    // Since hrId is hardcoded as 2, HR always sees all PENDING records
    List<EmployeePersonalDetails> findByVerificationStatus(VerificationStatus status);

    List<EmployeePersonalDetails> findAllByOrderBySubmittedAtDesc();
}