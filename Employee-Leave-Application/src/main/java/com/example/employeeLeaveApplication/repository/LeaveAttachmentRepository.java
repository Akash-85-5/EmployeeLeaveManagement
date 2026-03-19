package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {

    List<LeaveAttachment> findByLeaveApplicationId(Long leaveApplicationId);

    /**
     * Batch fetch attachments for multiple leave application IDs.
     * Optimized single query instead of N queries.
     */
    @Query("SELECT a FROM LeaveAttachment a WHERE a.leaveApplicationId IN :leaveIds")
    List<LeaveAttachment> findByLeaveApplicationIdIn(@Param("leaveIds") List<Long> leaveIds);

    void deleteByLeaveApplicationId(Long leaveApplicationId);
}