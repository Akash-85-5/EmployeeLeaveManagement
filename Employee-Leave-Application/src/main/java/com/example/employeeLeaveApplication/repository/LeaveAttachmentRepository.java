package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {
}
