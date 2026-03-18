package com.example.employeeLeaveApplication.component;

import com.example.employeeLeaveApplication.entity.HolidayCalendar;
import com.example.employeeLeaveApplication.repository.HolidayCalendarRepository;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DB-driven holiday checker — works for ANY year automatically.
 *
 * 1. Weekend detection → pure Java DayOfWeek, no config needed.
 * 2. Public holiday detection → reads from holiday_calendar table.
 *    No dates are hardcoded here.
 * 3. Cache → ConcurrentHashMap per date. Cleared when Admin
 *    adds/edits/deletes a holiday via HolidayCalendarService.
 */
@Component
public class HolidayChecker {
    private final HolidayCalendarRepository holidayRepo;
    // Per-date cache: true = holiday in DB, false = not a holiday
    private final Map<LocalDate, Boolean> cache = new ConcurrentHashMap<>();

    public HolidayChecker(HolidayCalendarRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    // Saturday or Sunday — works for any year, any date
    public boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    // Checks holiday_calendar table (DB-driven, cached)
    public boolean isPublicHoliday(LocalDate date) {
        return cache.computeIfAbsent(date, this::lookupInDb);
    }

    // Main method used by AttendanceScheduler and LeaveApplicationService
    // true = skip this date (no LOP, no leave deduction)
    public boolean isNonWorkingDay(LocalDate date) {
        return isWeekend(date) || isPublicHoliday(date);
    }

    // Returns the holiday record — useful when you need the holiday name
    public Optional<HolidayCalendar> getHolidayDetails(LocalDate date) {
        return holidayRepo.findByHolidayDateAndActiveTrue(date);
    }

    // Returns label: "Saturday" / "Sunday" / holiday name / null if working day
    public String getNonWorkingDayLabel(LocalDate date) {
        if (isWeekend(date)) {
            String name = date.getDayOfWeek().name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
        return holidayRepo.findByHolidayDateAndActiveTrue(date)
                .map(HolidayCalendar::getHolidayName)
                .orElse(null);
    }

    // Call this after Admin adds/edits/deletes any holiday
    public void invalidateCache() {
        cache.clear();
    }

    // Call this after Admin edits a single date
    public void invalidateCacheFor(LocalDate date) {
        cache.remove(date);
    }

    private boolean lookupInDb(LocalDate date) {
        return holidayRepo.findByHolidayDateAndActiveTrue(date).isPresent();
    }

    // Helper method for leave calculations (used by LeaveApplicationService)
    public long countWorkingDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (!isNonWorkingDay(cursor)) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
    public String getNonWorkingDayReason(LocalDate date) {
        // Check if it's a weekend
        if (isWeekend(date)) {
            DayOfWeek day = date.getDayOfWeek();
            String dayName = day.name();
            return dayName.charAt(0) + dayName.substring(1).toLowerCase() + " (weekend)";
        }

        // Check if it's a public holiday from database
        Optional<HolidayCalendar> holiday = holidayRepo.findByHolidayDateAndActiveTrue(date);
        if (holiday.isPresent()) {
            return holiday.get().getHolidayName() + " (public holiday)";
        }

        // It's a working day
        return null;
    }
}