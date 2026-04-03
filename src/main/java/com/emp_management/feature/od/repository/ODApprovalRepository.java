package com.emp_management.feature.od.repository;

import com.emp_management.feature.od.entity.ODApproval;
import com.emp_management.shared.enums.ApprovalLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for ODApproval audit records.
 * Mirrors LeaveApprovalRepository exactly.
 */
public interface ODApprovalRepository extends JpaRepository<ODApproval, Long> {

    Page<ODApproval> findByOdIdOrderByDecidedAtDesc(Long odId, Pageable pageable);

    Page<ODApproval> findByApproverIdOrderByDecidedAtDesc(String approverId, Pageable pageable);

    List<ODApproval> findByOdIdAndApprovalLevel(Long odId, ApprovalLevel approvalLevel);
}