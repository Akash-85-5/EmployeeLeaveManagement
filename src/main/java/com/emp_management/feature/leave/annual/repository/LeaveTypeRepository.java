package com.emp_management.feature.leave.annual.repository;

import com.emp_management.feature.leave.annual.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    List<LeaveType> findAllByAutoAllocateTrue();
    Optional<LeaveType> findByLeaveType(String leaveType);
}
