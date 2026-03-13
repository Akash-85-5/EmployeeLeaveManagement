package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.*;
import com.example.employeeLeaveApplication.enums.PayrollStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        if(!payslipRepository.findByYearAndMonth(year, month).isEmpty()){
            throw new RuntimeException(
                    "Payroll already generated for " + month + "/" + year +
                            ". Delete existing payroll to regenerate."
            );
        }

        LocalDate payrollDate = LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()
        );

        List<Employee> employees = employeeRepository.findAll();

        List<Payslip> payslips = new ArrayList<>();

        for (Employee emp : employees) {

            if (emp.getRole() == Role.HR) continue;

            if (payslipRepository.existsByEmployeeIdAndYearAndMonth(
                    emp.getId(), year, month)) continue;

            EmployeeSalary salary = salaryRepository
                    .findEffectiveSalary(emp.getId(), payrollDate)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (salary == null) continue;

            Role role = emp.getRole();

            if (role == Role.TEAM_LEADER) {
                role = Role.EMPLOYEE;
            }

            SalaryStructure structure =
                    structureRepository.findByRole(role)
                            .orElse(null);

            if (structure == null) continue;

            BigDecimal basic = salary.getBasicSalary();

            BigDecimal hra = structure.getHra();

            BigDecimal conveyance = structure.getConveyance();

            BigDecimal medical = structure.getMedical();

            BigDecimal other = structure.getOtherAllowance();

            BigDecimal pf = basic
                    .multiply(structure.getPfPercent())
                    .divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);

            BigDecimal esi = basic
                    .multiply(structure.getEsiPercent())
                    .divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);

            BigDecimal professionalTax = structure.getProfessionalTax();

            LossOfPayRecord lop =
                    lopRepository
                            .findByEmployeeIdAndYearAndMonth(
                                    emp.getId(), year, month)
                            .orElse(null);

            BigDecimal lopAmount = BigDecimal.ZERO;

            if (lop != null) {

                BigDecimal perDaySalary =
                        basic.divide(BigDecimal.valueOf(30),2,RoundingMode.HALF_UP);

                lopAmount =
                        perDaySalary.multiply(
                                BigDecimal.valueOf(lop.getExcessDays()));
            }

            BigDecimal gross = basic
                    .add(hra)
                    .add(conveyance)
                    .add(medical)
                    .add(other);

            BigDecimal deductions = pf
                    .add(esi)
                    .add(professionalTax)
                    .add(lopAmount);

            BigDecimal net = gross.subtract(deductions);

            Payslip payslip = new Payslip();

            payslip.setEmployeeId(emp.getId());
            payslip.setYear(year);
            payslip.setMonth(month);
            payslip.setBasicSalary(basic);
            payslip.setHra(hra);
            payslip.setConveyance(conveyance);
            payslip.setMedical(medical);
            payslip.setOtherAllowance(other);
            payslip.setPf(pf);
            payslip.setEsi(esi);
            payslip.setProfessionalTax(professionalTax);
            payslip.setLop(lopAmount);
            payslip.setNetSalary(net);
            payslip.setGeneratedDate(LocalDate.now());
            payslip.setStatus(PayrollStatus.PENDING);

            payslips.add(payslip);
        }

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