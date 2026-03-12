package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.enums.PayrollStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryRepository salaryRepository;
    private final SalaryStructureRepository structureRepository;
    private final PayslipRepository payslipRepository;
    private final LossOfPayRecordRepository lopRepository;

    public PayrollService(
            EmployeeRepository employeeRepository,
            EmployeeSalaryRepository salaryRepository,
            SalaryStructureRepository structureRepository,
            PayslipRepository payslipRepository,
            LossOfPayRecordRepository lopRepository) {

        this.employeeRepository = employeeRepository;
        this.salaryRepository = salaryRepository;
        this.structureRepository = structureRepository;
        this.payslipRepository = payslipRepository;
        this.lopRepository = lopRepository;
    }

    public void generatePayroll(Integer year, Integer month) {

        List<Employee> employees = employeeRepository.findAll();

        LocalDate payrollDate = LocalDate.of(year, month, 1);

        List<Payslip> payslips = new ArrayList<>();

        for (Employee emp : employees) {

            if (emp.getRole() == Role.HR) {
                continue;
            }

            if(payslipRepository.existsByEmployeeIdAndYearAndMonth(
                    emp.getId(), year, month)) {
                continue;
            }

            EmployeeSalary salary =
                    salaryRepository
                            .findEffectiveSalary(emp.getId(), payrollDate)
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (salary == null) {
                continue;
            }

            Role role = emp.getRole();

            if(role == Role.TEAM_LEADER){
                role = Role.EMPLOYEE;
            }

            SalaryStructure structure =
                    structureRepository
                            .findByRole(role)
                            .orElse(null);

            if (structure == null) {
                continue;
            }

            double basic = salary.getBasicSalary();

            double hra = structure.getHraAmount();

            double transport = structure.getTransportAllowance();

            double pf = basic * structure.getPfPercent() / 100;

            double tax = basic * structure.getTaxPercent() / 100;

            // Loss Of Pay calculation
            LossOfPayRecord lop =
                    lopRepository
                            .findByEmployeeIdAndYearAndMonth(
                                    emp.getId(), year, month)
                            .orElse(null);

            double lopAmount = 0;

            if (lop != null) {

                double perDaySalary = basic / 30;

                lopAmount = perDaySalary * lop.getExcessDays();
            }

            double gross = basic + hra + transport;

            double net = gross - (pf + tax + lopAmount);

            Payslip payslip = new Payslip();

            payslip.setEmployeeId(emp.getId());
            payslip.setYear(year);
            payslip.setMonth(month);
            payslip.setBasicSalary(basic);
            payslip.setHra(hra);
            payslip.setTransportAllowance(transport);
            payslip.setPfDeduction(pf);
            payslip.setTaxDeduction(tax);
            payslip.setLopDeduction(lopAmount);
            payslip.setNetSalary(net);
            payslip.setGeneratedDate(LocalDate.now());
            payslip.setStatus(PayrollStatus.PENDING);

            payslips.add(payslip);
        }

        // Save all payslips at once (performance improvement)
        payslipRepository.saveAll(payslips);
    }

    public void markPayrollPaid(Integer year, Integer month) {

        List<Payslip> payslips =
                payslipRepository.findByYearAndMonth(year, month);

        for (Payslip payslip : payslips) {

            payslip.setStatus(PayrollStatus.PAID);
        }

        payslipRepository.saveAll(payslips);
    }
}