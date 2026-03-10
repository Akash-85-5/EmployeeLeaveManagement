package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryRepository salaryRepository;
    private final SalaryStructureRepository structureRepository;
    private final PayslipRepository payslipRepository;
    private final LossOfPayRecordRepository lopRepository;

    public PayrollService(EmployeeRepository employeeRepository, EmployeeSalaryRepository salaryRepository, SalaryStructureRepository structureRepository, PayslipRepository payslipRepository, LossOfPayRecordRepository lopRepository){
        this.employeeRepository = employeeRepository;
        this.salaryRepository = salaryRepository;
        this.structureRepository = structureRepository;
        this.payslipRepository = payslipRepository;
        this.lopRepository = lopRepository;
    }

    public void generatePayroll(Integer year, Integer month) {

        List<Employee> employees = employeeRepository.findAll();

        LocalDate payrollDate = LocalDate.of(year, month, 1);

        for (Employee emp : employees) {

            if(payslipRepository.existsByEmployeeIdAndYearAndMonth(
                    emp.getId(), year, month)) {
                throw new RuntimeException(
                        "Payroll already generated for employee " + emp.getId()
                );
            }
            EmployeeSalary salary =
                    salaryRepository
                            .findEffectiveSalary(emp.getId(), payrollDate)
                            .stream()
                            .findFirst()
                            .orElseThrow();

            SalaryStructure structure =
                    structureRepository.findByRole(emp.getRole())
                            .orElseThrow();

            double basic = salary.getBasicSalary();

            double hra = structure.getHraAmount();

            double transport = structure.getTransportAllowance();

            double pf = basic * structure.getPfPercent() / 100;

            double tax = basic * structure.getTaxPercent() / 100;

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

            payslipRepository.save(payslip);
        }
    }
}
