// src/main/java/com/example/employeeLeaveApplication/repository/EmployeeRepository.java
package com.example.employeeLeaveApplication.feature.employee.repository;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.shared.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByreportingId(Long reportingId);

//    // NEW: find employees under a team leader
//    List<Employee> findByTeamLeaderId(Long teamLeaderId);

    List<Employee> findByRole(Role role);

    List<Employee> findByNameContainingIgnoreCase(String name);

    Long countByActive(Boolean active);

    List<Employee> findByActiveTrue();

    @Query("SELECT e FROM Employee e WHERE e.reportingId = :id AND e.active = true")
    List<Employee> findActiveTeamMembers(@Param("id") Long id);

    @Query("SELECT e FROM Employee e WHERE e.active = true")
    List<Employee> findActiveEmployees();

    @Query("SELECT e FROM Employee e " +
            "WHERE (e.biometricStatus = 'PENDING' OR e.vpnStatus = 'PENDING') " +
            "AND e.onboardingCompletedAt IS NULL " +
            "ORDER BY e.joiningDate ASC")
    List<Employee> findOnboardingPending();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.biometricStatus = 'PENDING' AND e.active = true")
    Integer countPendingBiometric();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.vpnStatus = 'PENDING' AND e.active = true")
    Integer countPendingVPN();

    @Query("SELECT e FROM Employee e WHERE e.reportingId = :reportingId AND e.active = true ORDER BY e.name ASC")
    List<Employee> findTeamMembersByManager(@Param("reportingId") Long reportingId);

    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'MANAGER' AND e.active = true")
    List<Employee> findAllManagers();

    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'HR' AND e.active = true")
    List<Employee> findAllHr();
//    @Query("SELECT e FROM Employee e WHERE e.teamLeaderId = :teamLeaderId AND e.active = true")
//    List<Employee> findActiveTeamMembersByTeamLeader(@Param("teamLeaderId") Long teamLeaderId);
}