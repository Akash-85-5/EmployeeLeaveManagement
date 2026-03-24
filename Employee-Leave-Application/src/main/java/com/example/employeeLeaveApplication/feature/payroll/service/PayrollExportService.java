package com.example.employeeLeaveApplication.feature.payroll.service;

import com.example.employeeLeaveApplication.feature.payroll.entity.Payslip;
import com.example.employeeLeaveApplication.shared.enums.PayrollStatus;
import com.example.employeeLeaveApplication.feature.payroll.repository.PayslipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayrollExportService {

    private final PayslipRepository payslipRepository;

    public PayrollExportService(PayslipRepository payslipRepository) {
        this.payslipRepository = payslipRepository;
    }

    public List<Payslip> getMonthlyPayroll(Integer year, Integer month) {

        return payslipRepository.findByYearAndMonthAndStatusNot(
                year,
                month,
                PayrollStatus.DELETED
        );
    }
}