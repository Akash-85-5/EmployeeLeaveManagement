package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.CompOffBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompOffBalanceRepository
        extends JpaRepository<CompOffBalance, Long> {

    Optional<CompOffBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);
}
