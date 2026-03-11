// src/main/java/com/example/employeeLeaveApplication/repository/LeaveApprovalRepository.java
package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveApproval;
import com.example.employeeLeaveApplication.enums.ApprovalLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

    Page<LeaveApproval> findByLeaveIdOrderByDecidedAtDesc(Long leaveId, Pageable pageable);

    Page<LeaveApproval> findByApproverIdOrderByDecidedAtDesc(Long approverId, Pageable pageable);

    List<LeaveApproval> findByLeaveIdAndApprovalLevel(Long leaveId, ApprovalLevel approvalLevel);
}