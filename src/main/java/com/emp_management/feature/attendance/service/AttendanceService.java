package com.emp_management.feature.attendance.service;

import com.emp_management.feature.attendance.dto.*;
import com.emp_management.feature.attendance.entity.AttendanceSummary;
import com.emp_management.feature.attendance.repository.AttendanceSummaryRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceSummaryRepository repo;

    public AttendanceService(AttendanceSummaryRepository repo) {
        this.repo = repo;
    }

    // 🔹 Employee Monthly Calendar
    public List<AttendanceCalendarDTO> getEmployeeMonthly(String empId, int year, int month) {

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return repo
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(empId, from, to)
                .stream()
                .map(this::mapToCalendar)
                .toList();
    }

    // 🔹 Daily View
    public List<AttendanceDetailDTO> getDailyAttendance(LocalDate date) {

        return repo
                .findByAttendanceDateOrderByEmployeeNameAsc(date)
                .stream()
                .map(this::mapToDetail)
                .toList();
    }

    // 🔹 All Employees (Pagination + Filter)
    public Page<AttendanceDetailDTO> getAllEmployeesAttendance(
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        return repo
                .findFilteredAttendance(status, from, to, pageable)
                .map(this::mapToDetail);
    }

    // 🔹 Punch Records (single day)
    public AttendanceCalendarDTO getPunchRecords(String empId, LocalDate date) {
        return repo.findByEmployeeIdAndAttendanceDate(empId, date)
                .map(this::mapToCalendar)
                .orElse(null);
    }
//
//    // 🔹 CORE FIX: Calculate working hours properly
//    private LocalTime calculateWorkingHours(LocalTime checkIn, LocalTime checkOut) {
//
//        if (checkIn == null || checkOut == null) {
//            return null;
//        }
//
//        Duration duration = Duration.between(checkIn, checkOut);
//
//        long hours = duration.toHours();
//        long minutes = duration.toMinutes() % 60;
//
//        return LocalTime.of((int) hours, (int) minutes);
//    }

    // 🔹 Mappers
    private AttendanceCalendarDTO mapToCalendar(AttendanceSummary att) {

        AttendanceCalendarDTO dto = new AttendanceCalendarDTO();

        dto.setDate(att.getAttendanceDate());
        dto.setStatus(att.getAttendanceStatus());
        dto.setCheckIn(att.getCheckIn());
        dto.setCheckOut(att.getCheckOut());
        dto.setWorkingHours(att.getWorkingHours());

        // ✅ Always compute instead of trusting DB blindly
//        dto.setWorkingHours(
//                calculateWorkingHours(att.getCheckIn(), att.getCheckOut())
//        );

        dto.setPunchRecords(att.getPunchRecords());

        return dto;
    }

    private AttendanceDetailDTO mapToDetail(AttendanceSummary att) {

        AttendanceDetailDTO dto = new AttendanceDetailDTO();

        dto.setEmployeeId(att.getEmployeeId());
        dto.setEmployeeName(att.getEmployeeName());
        dto.setDate(att.getAttendanceDate());
        dto.setStatus(att.getAttendanceStatus() != null
                ? att.getAttendanceStatus().trim()
                : null);
        dto.setCheckIn(att.getCheckIn());
        dto.setCheckOut(att.getCheckOut());
        dto.setWorkingHours(att.getWorkingHours());


        // ✅ Same fix here
//        dto.setWorkingHours(
//                calculateWorkingHours(att.getCheckIn(), att.getCheckOut())
//        );

        dto.setPunchRecords(att.getPunchRecords());
        dto.setLopTriggered(att.isLopTriggered());

        return dto;
    }
}