package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findByRole(Role role);

    // Search employees by name (case-insensitive, partial match)
    List<Employee> findByNameContainingIgnoreCase(String name);

    // Count active/inactive employees
    Long countByActive(Boolean active);

    List<Employee> findByActiveTrue();

    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true")
    List<Employee> findActiveTeamMembers(@Param("managerId") Long managerId);

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

    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true " +
            "ORDER BY e.name ASC")
    List<Employee> findTeamMembersByManager(@Param("managerId") Long managerId);

    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'MANAGER' AND e.active = true")
    List<Employee> findAllManagers();

}
