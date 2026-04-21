package com.emp_management.feature.leave.annual.repository;


import com.emp_management.feature.leave.annual.entity.LeaveApproval;
import com.emp_management.shared.enums.ApprovalLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

    Page<LeaveApproval> findByLeaveIdOrderByDecidedAtDesc(Long leaveId, Pageable pageable);

    Page<LeaveApproval> findByApproverIdOrderByDecidedAtDesc(String approverId, Pageable pageable);

    List<LeaveApproval> findByLeaveIdAndApprovalLevel(Long leaveId, ApprovalLevel approvalLevel);

    List<LeaveApproval> findByLeaveIdInOrderByDecidedAtAsc(List<Long> leaveIds);
}