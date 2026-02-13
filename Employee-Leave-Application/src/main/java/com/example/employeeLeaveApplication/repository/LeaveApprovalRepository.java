package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {
}
