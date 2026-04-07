package com.example.employeeLeaveApplication.feature.leave.carryforward.repository;

import com.example.employeeLeaveApplication.feature.leave.carryforward.entity.CarryForwardLeaveApplication;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarryForwardLeaveApplicationRepository
        extends JpaRepository<CarryForwardLeaveApplication, Long> {

    List<CarryForwardLeaveApplication> findByEmployeeId(Long employeeId);

    List<CarryForwardLeaveApplication> findByEmployeeIdAndYear(Long employeeId, Integer year);

    List<CarryForwardLeaveApplication> findByStatus(LeaveStatus status);

    List<CarryForwardLeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    List<CarryForwardLeaveApplication> findByEmployeeIdAndYearAndStatus(
            Long employeeId, Integer year, LeaveStatus status);

    /**
     * Sum of APPROVED days for an employee in a year.
     */
    @Query("""
           SELECT COALESCE(SUM(a.days), 0)
           FROM CarryForwardLeaveApplication a
           WHERE a.employeeId = :employeeId
             AND a.year       = :year
             AND a.status     = 'APPROVED'
           """)
    Double sumApprovedDays(@Param("employeeId") Long employeeId,
                           @Param("year") Integer year);

    /**
     * Pending applications waiting for a specific role to act.
     *
     * Logic:
     *   currentApprovalLevel = 1 → look at level1RequiredRole
     *   currentApprovalLevel = 2 → look at level2RequiredRole
     *
     * The approver's role is passed in — they see every PENDING application
     * where their role is the required approver at the current level.
     */
    @Query("""
           SELECT cfa
           FROM CarryForwardLeaveApplication cfa
           WHERE cfa.status = 'PENDING'
             AND (
               (cfa.currentApprovalLevel = 1 AND cfa.level1RequiredRole = :approverRole)
               OR
               (cfa.currentApprovalLevel = 2 AND cfa.level2RequiredRole = :approverRole)
             )
           ORDER BY cfa.createdAt ASC
           """)
    List<CarryForwardLeaveApplication> findPendingByApproverRole(
            @Param("approverRole") String approverRole);

    /**
     * Pending applications for a specific approver role, further filtered
     * to only the applicants who are direct reports of the given manager.
     *
     * Used when MANAGER-role approvers should only see their own team's queue,
     * not every employee in the company at that approval level.
     */
    @Query("""
           SELECT cfa
           FROM CarryForwardLeaveApplication cfa
           JOIN Employee e ON e.id = cfa.employeeId
           WHERE cfa.status = 'PENDING'
             AND e.managerId = :managerId
             AND (
               (cfa.currentApprovalLevel = 1 AND cfa.level1RequiredRole = :approverRole)
               OR
               (cfa.currentApprovalLevel = 2 AND cfa.level2RequiredRole = :approverRole)
             )
           ORDER BY cfa.createdAt ASC
           """)
    List<CarryForwardLeaveApplication> findPendingByManagerAndRole(
            @Param("managerId") Long managerId,
            @Param("approverRole") String approverRole);
}