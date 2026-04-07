package com.emp_management.feature.payroll.repository;

import com.emp_management.feature.payroll.entity.Payslip;
import com.emp_management.shared.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip,Long> {

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(
            String  employeeId,
            Integer year,
            Integer month
    );

    boolean existsByEmployeeIdAndYearAndMonth(
            String employeeId,
            Integer year,
            Integer month
    );

    List<Payslip> findByEmployeeIdAndStatus(
            String employeeId,
            PayrollStatus status
    );

    List<Payslip> findByYearAndMonthAndStatusNot(
            Integer year,
            Integer month,
            PayrollStatus status
    );
    List<Payslip> findByYearAndStatusNot(Integer year, PayrollStatus status);
    List<Payslip> findByEmployeeIdAndStatusNot(
            String employeeId,
            PayrollStatus status
    );
}
