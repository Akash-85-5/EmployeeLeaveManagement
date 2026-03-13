package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.GeneratePayslipRequest;
import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.enums.PayrollStatus;
import com.example.employeeLeaveApplication.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final LossOfPayRecordRepository lossOfPayRecordRepository;

    public PayslipService(
            PayslipRepository payslipRepository,
            LossOfPayRecordRepository lossOfPayRecordRepository) {

        this.payslipRepository = payslipRepository;
        this.lossOfPayRecordRepository = lossOfPayRecordRepository;
    }

    public Payslip generatePayslip(GeneratePayslipRequest request) {

        Payslip existing = payslipRepository
                .findByEmployeeIdAndYearAndMonth(
                        request.getEmployeeId(),
                        request.getYear(),
                        request.getMonth())
                .orElse(null);

        if (existing != null) {
            throw new RuntimeException("Payslip already generated for this month");
        }

        double basic = request.getBasicSalary();
        double hra = request.getHra();
        double transport = request.getTransportAllowance();

        double pf = basic * request.getPfPercent() / 100;
        double tax = basic * request.getTaxPercent() / 100;

        LossOfPayRecord lopRecord =
                lossOfPayRecordRepository
                        .findByEmployeeIdAndYearAndMonth(
                                request.getEmployeeId(),
                                request.getYear(),
                                request.getMonth())
                        .orElse(null);

        double lop = 0;

        if (lopRecord != null) {
            double excessDays = lopRecord.getExcessDays();
            double perDaySalary = basic / 30;
            lop = perDaySalary * excessDays;
        }

        double gross = basic + hra + transport;
        double net = gross - (pf + tax + lop);

        Payslip payslip = new Payslip();

        payslip.setEmployeeId(request.getEmployeeId());
        payslip.setYear(request.getYear());
        payslip.setMonth(request.getMonth());

        payslip.setBasicSalary(BigDecimal.valueOf(basic));
        payslip.setHra(BigDecimal.valueOf(hra));
        payslip.setConveyance(BigDecimal.valueOf(transport));

        payslip.setPf(BigDecimal.valueOf(pf));
        payslip.setProfessionalTax(BigDecimal.valueOf(tax));
        payslip.setLop(BigDecimal.valueOf(lop));

        payslip.setNetSalary(BigDecimal.valueOf(net));

        payslip.setGeneratedDate(LocalDate.now());
        payslip.setStatus(PayrollStatus.PENDING);

        return payslipRepository.save(payslip);
    }

    public Payslip updatePayslip(Long id, GeneratePayslipRequest request) {

        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        if (payslip.getStatus() == PayrollStatus.PAID) {
            throw new RuntimeException("Cannot modify payroll after it is paid");
        }

        payslip.setBasicSalary(BigDecimal.valueOf(request.getBasicSalary()));

        return payslipRepository.save(payslip);
    }
}