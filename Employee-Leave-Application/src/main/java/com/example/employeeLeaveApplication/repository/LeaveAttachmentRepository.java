package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {

    List<LeaveAttachment> findByLeaveApplicationId(Long leaveApplicationId);

    void deleteByLeaveApplicationId(Long leaveApplicationId);
}