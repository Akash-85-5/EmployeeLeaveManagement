package com.emp_management.feature.leave.carryforward.dto;

/**
 * One row per month — returned by GET /api/carryforward/monthly/{employeeId}?year=YYYY
 *
 * Fields:
 *   openingBalance  = carry-forward balance at start of this month
 *   used            = total approved CF leave days whose startDate is in this month
 *   closingBalance  = openingBalance − used  (carries into next month)
 */
public class CarryForwardMonthlyUsageResponse {

    private int    month;           // 1–12
    private String monthName;       // "January" … "December"
    private int    year;
    private double openingBalance;  // balance at start of month
    private double used;            // CF days consumed in this month
    private double closingBalance;  // remaining at end of month

    public CarryForwardMonthlyUsageResponse() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getMonth()          { return month; }
    public String getMonthName()      { return monthName; }
    public int    getYear()           { return year; }
    public double getOpeningBalance() { return openingBalance; }
    public double getUsed()           { return used; }
    public double getClosingBalance() { return closingBalance; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setMonth(int month)                   { this.month = month; }
    public void setMonthName(String monthName)         { this.monthName = monthName; }
    public void setYear(int year)                      { this.year = year; }
    public void setOpeningBalance(double openingBalance){ this.openingBalance = openingBalance; }
    public void setUsed(double used)                   { this.used = used; }
    public void setClosingBalance(double closingBalance){ this.closingBalance = closingBalance; }
}