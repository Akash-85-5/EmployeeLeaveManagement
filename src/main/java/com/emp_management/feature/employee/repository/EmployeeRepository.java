// src/main/java/com/example/employeeLeaveApplication/repository/EmployeeRepository.java
package com.emp_management.feature.employee.repository;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.shared.dto.EmployeeListDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    @Query("""
    SELECT e FROM Employee e
    WHERE LOWER(e.role.roleName) = LOWER(:roleName)
      AND e.active = true
""")
    List<Employee> findAllByRoleName(@Param("roleName") String roleName);

    @Query("SELECT e.empId AS empId, e.name AS empName, e.role.roleName AS role FROM Employee e WHERE e.active = true")
    List<EmployeeListDto> findAllActiveEmployeeBasicDetails();

    Optional<Employee> findByEmpId(String id);
//    Optional<Employee> findByEmail(String email);
//    List<Employee> findByTeamId(Long teamId);
    List<Employee> findByReportingId(String managerId);
//
////    // NEW: find employees under a team leader
////    List<Employee> findByTeamLeaderId(Long teamLeaderId);
//
//    List<Employee> findByRole(String role);
//
    List<Employee> findByEmpIdContainingIgnoreCase(String name);
//
    Long countByActive(Boolean active);
//
    List<Employee> findByActiveTrue();
//
//    @Query("SELECT e FROM Employee e WHERE e.managerId = :id AND e.active = true")
//    List<Employee> findActiveTeamMembers(@Param("id") Long id);
//
//    @Query("SELECT e FROM Employee e WHERE e.active = true")
//    List<Employee> findActiveEmployees();
//
//    @Query("SELECT e FROM Employee e " +
//            "WHERE (e.biometricStatus = 'PENDING' OR e.vpnStatus = 'PENDING') " +
//            "AND e.onboardingCompletedAt IS NULL " +
//            "ORDER BY e.joiningDate ASC")
//    List<Employee> findOnboardingPending();
//
//    @Query("SELECT COUNT(e) FROM Employee e WHERE e.biometricStatus = 'PENDING' AND e.active = true")
//    Integer countPendingBiometric();
//
//    @Query("SELECT COUNT(e) FROM Employee e WHERE e.vpnStatus = 'PENDING' AND e.active = true")
//    Integer countPendingVPN();
//
//    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true ORDER BY e.name ASC")
//    List<Employee> findTeamMembersByManager(@Param("managerId") Long managerId);
//
//    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'MANAGER' AND e.active = true")
//    List<Employee> findAllManagers();
//
//    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'HR' AND e.active = true")
//    List<Employee> findAllHr();
//    @Query("SELECT e FROM Employee e WHERE e.teamLeaderId = :teamLeaderId AND e.active = true")
//    List<Employee> findActiveTeamMembersByTeamLeader(@Param("teamLeaderId") Long teamLeaderId);


    // Find active team members (employees whose reportingId = this manager)
    @Query("""
    SELECT e FROM Employee e
    WHERE e.reportingId = :managerId
      AND e.active      = true
""")
    List<Employee> findActiveTeamMembers(@Param("managerId") String managerId);


    @Query("""
    SELECT e FROM Employee e 
    WHERE LOWER(e.empId) LIKE LOWER(CONCAT('%', :query, '%')) 
       OR LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) 
       OR LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%'))
""")
    List<Employee> searchByAnyField(@Param("query") String query);
    // All active employees
    @Query("SELECT e FROM Employee e WHERE e.active = true")
    List<Employee> findActiveEmployees();

    // All managers by role name
//    @Query("SELECT e FROM Employee e WHERE LOWER(e.role.name) = 'manager' AND e.active = true")
//    List<Employee> findAllManagers();

    // Team members under a specific manager (same as findActiveTeamMembers)
    @Query("""
    SELECT e FROM Employee e
    WHERE e.reportingId = :managerId
      AND e.active      = true
""")
    List<Employee> findTeamMembersByManager(@Param("managerId") String managerId);
    Optional<Employee> findByEmail(String email);
}