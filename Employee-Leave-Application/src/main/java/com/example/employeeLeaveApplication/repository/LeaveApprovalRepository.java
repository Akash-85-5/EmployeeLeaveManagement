package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

    // Get approval history for a leave
    Page<LeaveApproval> findByLeaveIdOrderByDecidedAtDesc(Long leaveId, Pageable pageable);

    // Get manager's past decisions with pagination
    Page<LeaveApproval> findByManagerIdOrderByDecidedAtDesc(Long managerId, Pageable pageable);
}