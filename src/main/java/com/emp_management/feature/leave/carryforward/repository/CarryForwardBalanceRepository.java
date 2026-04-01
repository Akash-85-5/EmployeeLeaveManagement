package com.emp_management.feature.leave.carryforward.repository;

import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarryForwardBalanceRepository extends JpaRepository<CarryForwardBalance, Long> {

    /**
     * Find carry forward balance for employee in specific year
     */
    Optional<CarryForwardBalance> findByEmployee_EmpIdAndYear(String employeeId, Integer year);

    /**
     * Find all carry forward balances for a year (HR view)
     */
    List<CarryForwardBalance> findByYear(Integer year);

    /**
     * Find all carry forward balances for employee (all years)
     */
//    List<CarryForwardBalance> findByEmployeeId(Long employeeId);

    /**
     * Check if carry forward exists for employee in year
     */
//    boolean existsByEmployeeIdAndYear(String employeeId, Integer year);

    /**
     * Delete carry forward for employee in year
     */
//    void deleteByEmployeeIdAndYear(Long employeeId, Integer year);
}