package com.emp_management.feature.leave.annual.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String formatLeaveDateRange(LocalDate start, LocalDate end) {
        String startDay = start.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String startDateStr = start.format(DATE_FORMATTER);

        if (start.equals(end)) {
            return String.format("%s (%s)", startDateStr, startDay);
        } else {
            String endDay = end.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String endDateStr = end.format(DATE_FORMATTER);
            return String.format("%s (%s) to %s (%s)", startDateStr, startDay, endDateStr, endDay);
        }
    }
}