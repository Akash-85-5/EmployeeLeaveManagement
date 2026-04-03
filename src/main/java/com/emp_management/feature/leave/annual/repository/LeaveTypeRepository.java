package com.emp_management.feature.leave.annual.repository;

import com.emp_management.feature.leave.annual.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByLeaveType(String leaveType);

    List<LeaveType> findAllByAutoAllocateTrue();

    // Fetch types that should be EXCLUDED from yearly totals
    // i.e., gender/marital restricted one-time leaves
    @Query("""
        SELECT lt FROM LeaveType lt
        WHERE lt.eligibleGender IS NOT NULL
           OR lt.marriedOnly = true
    """)
    List<LeaveType> findRestrictedLeaveTypes();
}
