package com.example.employeeLeaveApplication.feature.reports.service;

import com.example.employeeLeaveApplication.feature.leave.compoff.entity.CompOff;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
import com.example.employeeLeaveApplication.shared.enums.CompOffStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
import com.example.employeeLeaveApplication.shared.enums.LeaveType;
import com.example.employeeLeaveApplication.feature.leave.compoff.repository.CompOffRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportsService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final CompOffRepository compOffRepository;

    public ReportsService(LeaveApplicationRepository leaveApplicationRepository,
                          EmployeeRepository employeeRepository,
                          CompOffRepository compOffRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.employeeRepository = employeeRepository;
        this.compOffRepository = compOffRepository;
    }

    /**
     * 1. Annual Leave Summary Report
     * GET /api/reports/annual/{year}
     */
    public Map<String, Object> getAnnualLeaveSummary(Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        Map<String, Object> report = new HashMap<>();

        // Basic stats
        long totalEmployees = employeeRepository.count();
        final Integer finalYear = year;
        List<LeaveApplication> yearLeaves = leaveApplicationRepository
                .findByYearAndStatus(year, LeaveStatus.APPROVED);

        // Total approved leaves
        long totalLeavesApproved = yearLeaves.size();

        // Total days used
        double totalDaysUsed = yearLeaves.stream()
                .mapToDouble(l -> l.getDays() != null ? l.getDays().doubleValue() : 0.0)
                .sum();

        // Average per employee
        double avgDaysPerEmployee = totalEmployees > 0 ? totalDaysUsed / totalEmployees : 0;

        // Most used leave type
        Map<LeaveType, Long> leaveTypeCount = yearLeaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.counting()
                ));

        LeaveType mostUsedType = leaveTypeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Comp-off statistics
        double totalCompOffDays = compOffRepository.findAll().stream()
                .filter(c -> c.getStatus() == CompOffStatus.EARNED || c.getStatus() == CompOffStatus.USED)
                .mapToDouble(c -> c.getDays() != null ? c.getDays().doubleValue() : 0.0)
                .sum();


        // Monthly breakdown
        List<Map<String, Object>> monthlyBreakdown = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            List<LeaveApplication> monthLeaves = yearLeaves.stream()
                    .filter(l -> l.getStartDate().getMonthValue() == currentMonth)
                    .toList();

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("totalLeaves", monthLeaves.size());
            monthData.put("totalDays", monthLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum());

            monthlyBreakdown.add(monthData);
        }

        report.put("year", year);
        report.put("totalEmployees", totalEmployees);
        report.put("totalLeavesApproved", totalLeavesApproved);
        report.put("totalDaysUsed", totalDaysUsed);
        report.put("averageDaysPerEmployee", Math.round(avgDaysPerEmployee * 100.0) / 100.0);
        report.put("mostUsedLeaveType", mostUsedType != null ? mostUsedType.toString() : "N/A");
        report.put("totalCompOffDays", totalCompOffDays);
        report.put("byMonth", monthlyBreakdown);
        report.put("leaveTypeDistribution", leaveTypeCount);

        return report;
    }

    /**
     * 2. Employee Leave History
     * GET /api/reports/employee/{employeeId}/history
     */
    public Map<String, Object> getEmployeeLeaveHistory(Long employeeId, Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        List<LeaveApplication> leaves = leaveApplicationRepository.findByEmployeeId(employeeId);

        // Filter by year if provided
        final Integer finalYear = year;
        leaves = leaves.stream()
                .filter(l -> l.getYear() != null && l.getYear().equals(finalYear))
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .toList();

        // Calculate totals
        long totalLeaves = leaves.size();
        double totalDays = leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .mapToDouble(l -> l.getDays().doubleValue())
                .sum();

        // Group by status
        Map<LeaveStatus, Long> statusBreakdown = leaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getStatus,
                        Collectors.counting()
                ));

        // Group by type
        Map<LeaveType, Double> typeBreakdown = leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.summingDouble(l -> l.getDays().doubleValue())
                ));

        Map<String, Object> report = new HashMap<>();
        report.put("employeeId", employeeId);
        report.put("employeeName", employee.getName());
        report.put("year", year);
        report.put("totalLeaves", totalLeaves);
        report.put("totalDays", totalDays);
        report.put("statusBreakdown", statusBreakdown);
        report.put("typeBreakdown", typeBreakdown);
        report.put("history", leaves);

        return report;
    }

    /**
     * 3. Employee Leave Summary
     * GET /api/reports/employee/{employeeId}/summary
     */
    public Map<String, Object> getEmployeeLeaveSummary(Long employeeId, Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        List<LeaveApplication> approvedLeaves = leaveApplicationRepository
                .findByEmployeeIdAndStatusAndYear(employeeId, LeaveStatus.APPROVED, year);

        double totalDaysUsed = approvedLeaves.stream()
                .mapToDouble(l -> l.getDays().doubleValue())
                .sum();

        Map<LeaveType, Double> typeBreakdown = approvedLeaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.summingDouble(l -> l.getDays().doubleValue())
                ));

        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", employeeId);
        summary.put("employeeName", employee.getName());
        summary.put("year", year);
        summary.put("totalLeavesApproved", approvedLeaves.size());
        summary.put("totalDaysUsed", totalDaysUsed);
        summary.put("byLeaveType", typeBreakdown);

        return summary;
    }

    /**
     * 5. Manager Team Report
     * GET /api/reports/manager/{managerId}/team-report
     * ⚠️ CORRECTED METHOD NAME TO MATCH CONTROLLER
     */
    public Map<String, Object> getManagerTeamReport(Long managerId, Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        List<Employee> teamMembers = employeeRepository.findByreportingId(managerId);

        List<Map<String, Object>> teamData = new ArrayList<>();
        double totalTeamDays = 0;

        final Integer finalYear = year;
        for (Employee emp : teamMembers) {
            List<LeaveApplication> empLeaves = leaveApplicationRepository
                    .findByEmployeeIdAndStatusAndYear(emp.getId(), LeaveStatus.APPROVED, finalYear);

            double empTotalDays = empLeaves.stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            totalTeamDays += empTotalDays;

            Map<LeaveType, Double> empTypeBreakdown = empLeaves.stream()
                    .collect(Collectors.groupingBy(
                            LeaveApplication::getLeaveType,
                            Collectors.summingDouble(l -> l.getDays().doubleValue())
                    ));

            Map<String, Object> empData = new HashMap<>();
            empData.put("employeeId", emp.getId());
            empData.put("employeeName", emp.getName());
            empData.put("totalLeaves", empLeaves.size());
            empData.put("totalDays", empTotalDays);
            empData.put("byLeaveType", empTypeBreakdown);

            teamData.add(empData);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("managerId", managerId);
        report.put("year", year);
        report.put("teamSize", teamMembers.size());
        report.put("totalTeamDaysUsed", totalTeamDays);
        report.put("averageDaysPerEmployee", teamMembers.size() > 0 ?
                Math.round((totalTeamDays / teamMembers.size()) * 100.0) / 100.0 : 0);
        report.put("teamMembers", teamData);

        return report;
    }

    /**
     * 6. Leave Trends Report
     * GET /api/reports/trends
     */
    public Map<String, Object> getLeaveTrends(LocalDate startDate, LocalDate endDate) {
        List<LeaveApplication> leaves = leaveApplicationRepository.findAll().stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> !l.getStartDate().isBefore(startDate))
                .filter(l -> !l.getEndDate().isAfter(endDate))
                .toList();

        // Monthly trends
        Map<String, Long> monthlyTrends = new LinkedHashMap<>();
        LocalDate current = startDate.withDayOfMonth(1);

        while (!current.isAfter(endDate)) {
            final LocalDate monthStart = current;
            final LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());

            long monthCount = leaves.stream()
                    .filter(l -> !l.getStartDate().isAfter(monthEnd) && !l.getEndDate().isBefore(monthStart))
                    .count();

            String monthKey = monthStart.getYear() + "-" +
                    String.format("%02d", monthStart.getMonthValue());
            monthlyTrends.put(monthKey, monthCount);

            current = current.plusMonths(1);
        }

        // Peak day analysis
        Map<LocalDate, Long> dailyCounts = new HashMap<>();
        for (LeaveApplication leave : leaves) {
            LocalDate date = leave.getStartDate();
            while (!date.isAfter(leave.getEndDate())) {
                dailyCounts.put(date, dailyCounts.getOrDefault(date, 0L) + 1);
                date = date.plusDays(1);
            }
        }

        LocalDate peakDay = dailyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Most common leave type
        Map<LeaveType, Long> typeDistribution = leaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.counting()
                ));

        LeaveType mostCommonType = typeDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<String, Object> report = new HashMap<>();
        report.put("period", startDate + " to " + endDate);
        report.put("totalLeaves", leaves.size());
        report.put("monthlyTrends", monthlyTrends);
        report.put("peakDay", peakDay);
        report.put("peakDayCount", peakDay != null ? dailyCounts.get(peakDay) : 0);
        report.put("mostCommonType", mostCommonType);
        report.put("typeDistribution", typeDistribution);

        return report;
    }

    /**
     * 7. Leave Type Distribution Report
     * GET /api/reports/distribution/leave-types
     */
    public Map<String, Object> getLeaveTypeDistribution(Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }

        final Integer finalYear = year;
        List<LeaveApplication> leaves = leaveApplicationRepository
                .findByYearAndStatus(year, LeaveStatus.APPROVED);
        Map<LeaveType, Long> countByType = leaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType, Collectors.counting()));

        Map<LeaveType, Double> daysByType = leaves.stream()
                .filter(l -> l.getYear() != null && l.getYear().equals(finalYear))
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.summingDouble(l -> l.getDays().doubleValue())
                ));

        // Calculate percentages
        long totalCount = countByType.values().stream().mapToLong(Long::longValue).sum();
        double totalDays = daysByType.values().stream().mapToDouble(Double::doubleValue).sum();

        Map<LeaveType, Double> percentageByType = new HashMap<>();
        if (totalCount > 0) {
            for (Map.Entry<LeaveType, Long> entry : countByType.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalCount;
                percentageByType.put(entry.getKey(), Math.round(percentage * 100.0) / 100.0);
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("totalLeaves", totalCount);
        report.put("totalDays", totalDays);
        report.put("countByType", countByType);
        report.put("daysByType", daysByType);
        report.put("percentageByType", percentageByType);

        return report;
    }

    /**
     * 8. Monthly Report
     * GET /api/reports/monthly/{year}/{month}
     * ⚠️ CORRECTED METHOD NAME TO MATCH CONTROLLER
     */
    public Map<String, Object> getMonthlyReport(Integer year, Integer month) {
        List<LeaveApplication> monthLeaves = leaveApplicationRepository
                .findByYearAndMonthAndStatus(year, month, LeaveStatus.APPROVED);

        long totalLeaves = monthLeaves.size();
        double totalDays = monthLeaves.stream()
                .mapToDouble(l -> l.getDays().doubleValue())
                .sum();

        Map<LeaveType, Long> typeBreakdown = monthLeaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.counting()
                ));

        Map<LeaveType, Double> daysByType = monthLeaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        Collectors.summingDouble(l -> l.getDays().doubleValue())
                ));

        // Peak day in the month
        Map<Integer, Long> dailyDistribution = monthLeaves.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getStartDate().getDayOfMonth(),
                        Collectors.counting()
                ));

        Integer peakDay = dailyDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("monthName", java.time.Month.of(month).name());
        report.put("totalLeaves", totalLeaves);
        report.put("totalDays", totalDays);
        report.put("countByType", typeBreakdown);
        report.put("daysByType", daysByType);
        report.put("peakDay", peakDay);
        report.put("dailyDistribution", dailyDistribution);
        report.put("leaves", monthLeaves);

        return report;
    }

    /**
     * 10. Comp-off Utilization Report
     * GET /api/reports/compoff/utilization
     * ⚠️ CORRECTED METHOD NAME AND SIGNATURE TO MATCH CONTROLLER
     */
    public Map<String, Object> getCompOffUtilizationReport() {
        List<CompOff> allCompOffs = compOffRepository.findAll();

        long totalEarned = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.EARNED)
                .count();

        long totalUsed = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.USED)
                .count();

        long totalPending = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.PENDING)
                .count();

        double earnedDays = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.EARNED)
                .mapToDouble(c -> c.getDays().doubleValue())
                .sum();

        double usedDays = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.USED)
                .mapToDouble(c -> c.getDays().doubleValue())
                .sum();

        double pendingDays = allCompOffs.stream()
                .filter(c -> c.getStatus() == CompOffStatus.PENDING)
                .mapToDouble(c -> c.getDays().doubleValue())
                .sum();

        double utilizationRate = earnedDays > 0 ? (usedDays / earnedDays) * 100 : 0;

        // Employee-wise breakdown
        Map<Long, List<CompOff>> employeeCompOffs = allCompOffs.stream()
                .collect(Collectors.groupingBy(CompOff::getEmployeeId));

        List<Map<String, Object>> employeeUtilization = new ArrayList<>();
        for (Map.Entry<Long, List<CompOff>> entry : employeeCompOffs.entrySet()) {
            Long empId = entry.getKey();
            List<CompOff> empCompOffs = entry.getValue();

            Employee emp = employeeRepository.findById(empId).orElse(null);

            double empEarned = empCompOffs.stream()
                    .filter(c -> c.getStatus() == CompOffStatus.EARNED)
                    .mapToDouble(c -> c.getDays().doubleValue())
                    .sum();

            double empUsed = empCompOffs.stream()
                    .filter(c -> c.getStatus() == CompOffStatus.USED)
                    .mapToDouble(c -> c.getDays().doubleValue())
                    .sum();

            Map<String, Object> empData = new HashMap<>();
            empData.put("employeeId", empId);
            empData.put("employeeName", emp != null ? emp.getName() : "Unknown");
            empData.put("earned", empEarned);
            empData.put("used", empUsed);
            empData.put("available", empEarned - empUsed);
            empData.put("utilizationRate", empEarned > 0 ?
                    Math.round((empUsed / empEarned) * 100 * 100.0) / 100.0 : 0);

            employeeUtilization.add(empData);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalEarnedCount", totalEarned);
        report.put("totalUsedCount", totalUsed);
        report.put("totalPendingCount", totalPending);
        report.put("earnedDays", earnedDays);
        report.put("usedDays", usedDays);
        report.put("pendingDays", pendingDays);
        report.put("availableDays", earnedDays - usedDays);
        report.put("utilizationRate", Math.round(utilizationRate * 100.0) / 100.0);
        report.put("employeeUtilization", employeeUtilization);

        return report;
    }
}