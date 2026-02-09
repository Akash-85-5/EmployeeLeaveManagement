package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LeaveApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {
}
