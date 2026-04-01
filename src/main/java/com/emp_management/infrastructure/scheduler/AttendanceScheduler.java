//package com.emp_management.infrastructure.scheduler;
//
//import com.example.employeeLeaveApplication.feature.attendance.entity.AttendanceSummary;
//import com.example.employeeLeaveApplication.feature.attendance.entity.BiometricLog;
//import com.example.employeeLeaveApplication.feature.attendance.repository.AttendanceSummaryRepository;
//import com.example.employeeLeaveApplication.feature.attendance.repository.BiometricLogRepository;
//import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
//import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
//import com.example.employeeLeaveApplication.feature.holiday.utils.HolidayChecker;
//import com.example.employeeLeaveApplication.feature.leave.annual.entity.LeaveApplication;
//import com.example.employeeLeaveApplication.feature.leave.annual.repository.LeaveApplicationRepository;
//import com.example.employeeLeaveApplication.feature.leave.lop.entity.LopRecord;
//import com.example.employeeLeaveApplication.feature.leave.lop.repository.LopRecordRepository;
//import com.example.employeeLeaveApplication.feature.workfromhome.entity.WorkFromHome;
//import com.example.employeeLeaveApplication.feature.workfromhome.repository.WorkFromHomeRepository;
//import com.example.employeeLeaveApplication.shared.enums.LeaveStatus;
//import com.example.employeeLeaveApplication.shared.enums.WfhStatus;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Optional;
//
///**
// * Runs every day at 7:00 AM.
// * Processes YESTERDAY's attendance for all active employees.
// *
// * Decision order (first match wins — no LOP for matched case):
// *   1. Employee active?
// *   2. Joining date valid?
// *   3. Weekend?       → WEEKEND
// *   4. Public holiday? → HOLIDAY
// *   5. WFH approved?  → WFH
// *   6. Leave approved? → ON_LEAVE
// *   7. Biometric punch? → PRESENT or ABSENT
// *      ABSENT → insert lop_records
// *
// * HR and CFO are skipped — they never get LOP.
// */
//@Component
//public class AttendanceScheduler {
//
//    private static final Logger log = LoggerFactory.getLogger(AttendanceScheduler.class);
//
//    private final EmployeeRepository employeeRepo;
//    private final BiometricLogRepository bioRepo;
//    private final LeaveApplicationRepository leaveRepo;
//    private final WorkFromHomeRepository wfhRepo;
//    private final AttendanceSummaryRepository attRepo;
//    private final LopRecordRepository lopRepo;
//    private final HolidayChecker holidayChecker;
//
//    public AttendanceScheduler(EmployeeRepository          employeeRepo,
//                               BiometricLogRepository      bioRepo,
//                               LeaveApplicationRepository  leaveRepo,
//                               WorkFromHomeRepository      wfhRepo,
//                               AttendanceSummaryRepository attRepo,
//                               LopRecordRepository         lopRepo,
//                               HolidayChecker              holidayChecker) {
//        this.employeeRepo   = employeeRepo;
//        this.bioRepo        = bioRepo;
//        this.leaveRepo      = leaveRepo;
//        this.wfhRepo        = wfhRepo;
//        this.attRepo        = attRepo;
//        this.lopRepo        = lopRepo;
//        this.holidayChecker = holidayChecker;
//    }
//
//    @Scheduled(cron = "0 0 7 * * ?")
//    @Transactional
//    public void processYesterdayAttendance() {
//        LocalDate yesterday = LocalDate.now().minusDays(1);
//        log.info("=== Attendance Scheduler START — processing {} ===", yesterday);
//
//        List<Employee> employees = employeeRepo.findAll();
//        for (Employee emp : employees) {
//            try {
//                processEmployee(emp, yesterday);
//            } catch (Exception ex) {
//                log.error("Error processing employee {} on {}: {}",
//                        emp.getId(), yesterday, ex.getMessage());
//            }
//        }
//
//        log.info("=== Attendance Scheduler DONE — {} ===", yesterday);
//    }
//
//    private void processEmployee(Employee emp, LocalDate date) {
//        Long   empId   = emp.getId();
//        String empName = emp.getName();
//        String empRole = emp.getRole().name();
//
//        // Skip HR and CFO — they never get LOP
//        if (empRole.equals("HR") || empRole.equals("CFO")) {
//            return;
//        }
//
//        // 1. Active check
//        if (!emp.isActive()) return;
//
//        // 2. Joining date check
//        if (emp.getJoiningDate() == null || date.isBefore(emp.getJoiningDate())) {
//            saveAttendance(empId, empName, date, "NOT_JOINED", null, null, null, null, false);
//            return;
//        }
//
//        // 3. Weekend
//        if (holidayChecker.isWeekend(date)) {
//            saveAttendance(empId, empName, date, "WEEKEND", null, null, null, null, false);
//            return;
//        }
//
//        // 4. Public holiday
//        if (holidayChecker.isPublicHoliday(date)) {
//            saveAttendance(empId, empName, date, "HOLIDAY", null, null, null, null, false);
//            return;
//        }
//
//        // 5. WFH approved
//        Optional<WorkFromHome> wfh = wfhRepo
//                .findApprovedWfhForEmployeeOnDate(empId, date, WfhStatus.APPROVED);
//        if (wfh.isPresent()) {
//            saveAttendance(empId, empName, date, "WFH", null, null, null, wfh.get().getId(), false);
//            return;
//        }
//
//        // 6. Leave approved
//        Optional<LeaveApplication> leave = leaveRepo
//                .findApprovedLeaveForEmployeeOnDate(empId, date, LeaveStatus.APPROVED);
//        if (leave.isPresent()) {
//            saveAttendance(empId, empName, date, "ON_LEAVE", null, null, leave.get().getId(), null, false);
//            return;
//        }
//
//        // 7. Biometric punch
//        boolean hasPunch = bioRepo.existsByEmployeeIdAndPunchDate(empId, date);
//
//        if (hasPunch) {
//            Optional<BiometricLog> firstPunch = bioRepo.findFirstPunch(empId, date);
//            Optional<BiometricLog> lastPunch  = bioRepo.findLastPunch(empId, date);
//
//            LocalTime checkIn  = firstPunch.map(b -> b.getPunchTime().toLocalTime()).orElse(null);
//            LocalTime checkOut = lastPunch .map(b -> b.getPunchTime().toLocalTime()).orElse(null);
//            Long      inId     = firstPunch.map(BiometricLog::getId).orElse(null);
//            Long      outId    = lastPunch .map(BiometricLog::getId).orElse(null);
//
//            AttendanceSummary att = saveAttendance(
//                    empId, empName, date, "PRESENT", checkIn, checkOut, null, null, false);
//            att.setBiometricInId(inId);
//            att.setBiometricOutId(outId);
//
//            if (checkIn != null && checkOut != null) {
//                double hours = (checkOut.getHour() * 60 + checkOut.getMinute()
//                        - checkIn.getHour()  * 60 - checkIn.getMinute()) / 60.0;
//                att.setWorkingHours(hours);
//            }
//            attRepo.save(att);
//
//        } else {
//            // ABSENT → insert LOP
//            AttendanceSummary att = saveAttendance(
//                    empId, empName, date, "ABSENT", null, null, null, null, true);
//            insertLop(empId, empName, empRole, date, att.getId());
//        }
//    }
//
//    private AttendanceSummary saveAttendance(Long empId, String empName,
//                                             LocalDate date, String status,
//                                             LocalTime in, LocalTime out,
//                                             Long leaveId, Long wfhId,
//                                             boolean lopTriggered) {
//        AttendanceSummary att = new AttendanceSummary();
//        att.setEmployeeId(empId);
//        att.setEmployeeName(empName);
//        att.setAttendanceDate(date);
//        att.setAttendanceStatus(status);
//        att.setCheckIn(in);
//        att.setCheckOut(out);
//        att.setLeaveId(leaveId);
//        att.setWfhId(wfhId);
//        att.setLopTriggered(lopTriggered);
//        return attRepo.save(att);
//    }
//
//    private void insertLop(Long empId, String empName, String empRole,
//                           LocalDate date, Long attendanceSummaryId) {
//        if (lopRepo.existsByEmployeeIdAndLopDateAndReversedFalse(empId, date)) {
//            log.warn("LOP already exists for employee {} on {} — skipping", empId, date);
//            return;
//        }
//        LopRecord lop = new LopRecord();
//        lop.setEmployeeId(empId);
//        lop.setEmployeeName(empName);
//        lop.setEmployeeRole(empRole);
//        lop.setLopDate(date);
//        lop.setLopDays(1.0);
//        lop.setLopReason("NO_PUNCH");
//        lop.setAttendanceSummaryId(attendanceSummaryId);
//        lopRepo.save(lop);
//        log.info("LOP inserted → employee: {} ({}) date: {}", empName, empId, date);
//    }
//}
