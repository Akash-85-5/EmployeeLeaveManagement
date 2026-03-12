package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query("""
SELECT p FROM Payslip p
WHERE (p.year > :startYear OR (p.year = :startYear AND p.month >= :startMonth))
AND (p.year < :endYear OR (p.year = :endYear AND p.month <= :endMonth))
""")
    List<Payslip> findPayslipsBetweenDates(
            Integer startYear,
            Integer startMonth,
            Integer endYear,
            Integer endMonth
    );
    Optional<Payslip> findByEmployeeIdAndYearAndMonth(
            Long employeeId,
            Integer year,
            Integer month
    );

    List<Payslip> findByEmployeeId(Long employeeId);

    List<Payslip> findByYearAndMonth(Integer year, Integer month);

    boolean existsByEmployeeIdAndYearAndMonth(
            Long employeeId,
            Integer year,
            Integer month
    );
}