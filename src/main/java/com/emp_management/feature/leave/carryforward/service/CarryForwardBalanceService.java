package com.emp_management.feature.leave.carryforward.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveType;
import com.emp_management.feature.leave.annual.repository.AnnualLeaveMonthlyBalanceRepository;
import com.emp_management.feature.leave.annual.repository.LeaveApplicationRepository;
import com.emp_management.feature.leave.annual.repository.LeaveTypeRepository;
import com.emp_management.feature.leave.carryforward.dto.*;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;
import com.emp_management.feature.leave.carryforward.mapper.CarryForwardBalanceMapper;
import com.emp_management.feature.leave.carryforward.repository.CarryForwardBalanceRepository;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class CarryForwardBalanceService {

    private static final Logger log = LoggerFactory.getLogger(CarryForwardBalanceService.class);

    private final CarryForwardBalanceRepository carryForwardRepo;
    private final EmployeeRepository employeeRepository;
    private final AnnualLeaveMonthlyBalanceRepository annualMonthlyRepo;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public CarryForwardBalanceService(
            CarryForwardBalanceRepository carryForwardRepo,
            EmployeeRepository employeeRepository,
            AnnualLeaveMonthlyBalanceRepository annualMonthlyRepo,
            LeaveApplicationRepository leaveApplicationRepository,
            LeaveTypeRepository leaveTypeRepository) {
        this.carryForwardRepo = carryForwardRepo;
        this.employeeRepository = employeeRepository;
        this.annualMonthlyRepo = annualMonthlyRepo;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // ================= BALANCE =================

    @Transactional(readOnly = true)
    public CarryForwardBalanceResponse getBalance(String employeeId, int year) {
        Employee emp = requireEmployee(employeeId);

        return carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .map(CarryForwardBalanceMapper::toDTO)                      // record exists → map it
                .orElseGet(() -> CarryForwardBalanceMapper.toEmptyDTO(emp, year)); // no record → zeros
    }

    // ================= ELIGIBILITY =================

    @Transactional(readOnly = true)
    public CarryForwardEligibilityResponse checkEligibility(String employeeId, int year) {
        Employee emp = requireEmployee(employeeId);

        double maxCap = getMaxCarryForwardCap();

        double decemberRemaining = annualMonthlyRepo
                .findByEmployeeIdAndYearAndMonth(employeeId, year, 12)
                .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                .orElse(0.0);

        double eligibleAmount = Math.min(decemberRemaining, maxCap);

        double carriedIn = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .map(CarryForwardBalance::getTotalCarriedForward)
                .orElse(0.0);

        CarryForwardEligibilityResponse response = new CarryForwardEligibilityResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(emp.getName());
        response.setYear(year);
        response.setYearlyAllocated(maxCap);
        response.setBalance(decemberRemaining);
        response.setEligibleAmount(eligibleAmount);
        response.setCarriedIn(carriedIn);
        response.setEligible(eligibleAmount > 0);
        response.setReason(eligibleAmount > 0
                ? "Employee has " + decemberRemaining + " unused days. "
                + eligibleAmount + " days will carry forward."
                : "No unused annual leave to carry forward.");

        return response;
    }

    // ================= ALL BALANCES =================

    @Transactional(readOnly = true)
    public List<CarryForwardBalanceResponse> getAllBalances(int year) {
        return carryForwardRepo.findByYear(year).stream()
                .map(CarryForwardBalanceMapper::toDTO)      // mapper used here now
                .toList();
    }

    // ================= MONTHLY =================

    @Transactional(readOnly = true)
    public List<CarryForwardMonthlyUsageResponse> getMonthlyUsage(String employeeId, int year) {
        requireEmployee(employeeId);

        double openingBalance = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .map(CarryForwardBalance::getTotalCarriedForward)
                .orElse(0.0);

        List<LeaveApplication> cfLeaves = leaveApplicationRepository
                .findByEmployee_EmpIdAndLeaveType_LeaveTypeAndStatus(
                        employeeId, "CARRY_FORWARD", RequestStatus.APPROVED);

        Map<Integer, Double> usedByMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) usedByMonth.put(m, 0.0);

        for (LeaveApplication la : cfLeaves) {
            if (la.getStartDate() == null) continue;
            if (la.getStartDate().getYear() != year) continue;

            int month = la.getStartDate().getMonthValue();
            double days = la.getDays() != null ? la.getDays().doubleValue() : 0.0;
            usedByMonth.put(month, usedByMonth.get(month) + days);
        }

        List<CarryForwardMonthlyUsageResponse> result = new ArrayList<>();
        double runningRemaining = openingBalance;

        for (int m = 1; m <= 12; m++) {
            double used = usedByMonth.get(m);
            double openingThisMonth = runningRemaining;
            runningRemaining = Math.max(0.0, runningRemaining - used);

            CarryForwardMonthlyUsageResponse row = new CarryForwardMonthlyUsageResponse();
            row.setMonth(m);
            row.setMonthName(Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            row.setYear(year);
            row.setOpeningBalance(openingThisMonth);
            row.setUsed(used);
            row.setClosingBalance(runningRemaining);
            result.add(row);
        }

        return result;
    }

    // ================= AVAILABLE =================

    @Transactional(readOnly = true)
    public double getAvailableBalance(String employeeId, int year) {
        return carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .map(CarryForwardBalance::getRemaining)
                .orElse(0.0);
    }

    // ================= DEDUCT =================

    @Transactional
    public void deductLeave(String employeeId, int year, double days) {
        CarryForwardBalance balance = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .orElseThrow(() -> new BadRequestException(
                        "No carry-forward balance found for employee " + employeeId));

        if (days > balance.getRemaining()) {
            throw new BadRequestException("Insufficient CARRY_FORWARD balance");
        }

        balance.setTotalUsed(balance.getTotalUsed() + days);
        balance.setRemaining(balance.getRemaining() - days);
        carryForwardRepo.save(balance);
    }

    // ================= RESTORE =================

    @Transactional
    public void restoreLeave(String employeeId, int year, double days) {
        CarryForwardBalance balance = carryForwardRepo
                .findByEmployee_EmpIdAndYear(employeeId, year)
                .orElseThrow(() -> new BadRequestException(
                        "No carry-forward balance found for employee " + employeeId));

        balance.setTotalUsed(Math.max(0.0, balance.getTotalUsed() - days));
        balance.setRemaining(balance.getRemaining() + days);
        carryForwardRepo.save(balance);
    }

    // ================= YEAR END =================

    @Transactional
    public void processYearEndCarryForward(int previousYear) {

        double maxCap = getMaxCarryForwardCap();
        int currentYear = previousYear + 1;

        List<Employee> employees = employeeRepository.findAll();

        for (Employee emp : employees) {

            double decemberRemaining = annualMonthlyRepo
                    .findByEmployeeIdAndYearAndMonth(emp.getEmpId(), previousYear, 12)
                    .map(AnnualLeaveMonthlyBalance::getRemainingDays)
                    .orElse(0.0);

            double carryAmount = Math.min(decemberRemaining, maxCap);

            if (carryAmount <= 0.0) continue;

            CarryForwardBalance balance = carryForwardRepo
                    .findByEmployee_EmpIdAndYear(emp.getEmpId(), currentYear)
                    .orElseGet(() -> {
                        CarryForwardBalance b = new CarryForwardBalance();
                        b.setEmployee(emp);
                        b.setYear(currentYear);
                        b.setTotalUsed(0.0);
                        return b;
                    });

            balance.setTotalCarriedForward(carryAmount);
            balance.setRemaining(carryAmount - balance.getTotalUsed());
            carryForwardRepo.save(balance);
        }
    }

    // ================= HELPERS =================

    private double getMaxCarryForwardCap() {
        return leaveTypeRepository.findByLeaveType("CARRY_FORWARD")
                .map(LeaveType::getAllocatedDays)
                .map(val -> val.doubleValue())
                .orElse(10.0);
    }

    private Employee requireEmployee(String employeeId) {
        return employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
    }
}