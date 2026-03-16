package com.example.employeeLeaveApplication.component;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;

@Component
public class HolidayChecker {

    public boolean isNonWorkingDay(LocalDate date) {
        return getNonWorkingDayReason(date) != null;
    }
    public String getNonWorkingDayReason(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) return "Saturday (weekend)";
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY)   return "Sunday (weekend)";

        MonthDay md = MonthDay.from(date);
        if (md.equals(MonthDay.of(1,  1)))  return "New Year's Day (Jan 1)";
        if (md.equals(MonthDay.of(1,  26))) return "Republic Day (Jan 26)";
        if (md.equals(MonthDay.of(8,  15))) return "Independence Day (Aug 15)";
        if (md.equals(MonthDay.of(10, 2)))  return "Gandhi Jayanti (Oct 2)";
        if (md.equals(MonthDay.of(12, 25))) return "Christmas Day (Dec 25)";

        return null;
    }

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
}