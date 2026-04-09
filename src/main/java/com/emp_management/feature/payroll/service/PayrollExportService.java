package com.emp_management.feature.payroll.service;

import com.emp_management.feature.payroll.entity.Payslip;
import com.emp_management.feature.payroll.repository.PayslipRepository;
import com.emp_management.shared.enums.PayrollStatus;
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
