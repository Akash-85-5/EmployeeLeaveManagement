package com.example.employeeLeaveApplication.feature.payroll.repository;

import com.example.employeeLeaveApplication.feature.payroll.entity.Payslip;
import com.example.employeeLeaveApplication.shared.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip,Long> {

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(
            Long employeeId,
            Integer year,
            Integer month
    );

    boolean existsByEmployeeIdAndYearAndMonth(
            Long employeeId,
            Integer year,
            Integer month
    );

    List<Payslip> findByEmployeeIdAndStatus(
            Long employeeId,
            PayrollStatus status
    );

    List<Payslip> findByYearAndMonthAndStatusNot(
            Integer year,
            Integer month,
            PayrollStatus status
    );

}