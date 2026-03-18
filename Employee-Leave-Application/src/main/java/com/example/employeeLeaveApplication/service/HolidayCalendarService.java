package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.component.HolidayChecker;
import com.example.employeeLeaveApplication.entity.HolidayCalendar;
import com.example.employeeLeaveApplication.repository.HolidayCalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin-managed holiday CRUD.
 * Every write operation calls holidayChecker.invalidateCacheFor()
 * so the scheduler always reads fresh data on the next run.
 */
@Service
public class HolidayCalendarService {

    private final HolidayCalendarRepository repo;
    private final HolidayChecker            holidayChecker;

    public HolidayCalendarService(HolidayCalendarRepository repo,
                                  HolidayChecker holidayChecker) {
        this.repo           = repo;
        this.holidayChecker = holidayChecker;
    }

    // ── READ ──────────────────────────────────────────────────────

    // All active holidays for a year (used by UI calendar + scheduler)
    public List<HolidayCalendar> getHolidaysForYear(int year) {
        return repo.findByYear(year);
    }

    // All holidays including inactive (Admin management screen)
    public List<HolidayCalendar> getAllHolidaysForYear(int year) {
        return repo.findByYearAll(year);
    }

    // ── CREATE ────────────────────────────────────────────────────

    @Transactional
    public HolidayCalendar addHoliday(LocalDate date, String name,
                                      String type, Long createdBy) {
        if (repo.existsByHolidayDate(date)) {
            throw new RuntimeException(
                    "Holiday already exists for date: " + date +
                            ". Use update to edit it.");
        }

        HolidayCalendar h = new HolidayCalendar();
        h.setHolidayDate(date);
        h.setHolidayName(name);
        h.setHolidayType(type != null ? type : "NATIONAL");
        h.setActive(true);
        h.setCreatedBy(createdBy);

        HolidayCalendar saved = repo.save(h);
        holidayChecker.invalidateCacheFor(date);
        return saved;
    }

    // ── UPDATE ────────────────────────────────────────────────────

    @Transactional
    public HolidayCalendar updateHoliday(Long id, String name,
                                         String type, boolean active) {
        HolidayCalendar h = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found: " + id));

        h.setHolidayName(name);
        h.setHolidayType(type);
        h.setActive(active);

        HolidayCalendar saved = repo.save(h);
        holidayChecker.invalidateCacheFor(h.getHolidayDate());
        return saved;
    }

    // ── SOFT DELETE (sets active = false) ─────────────────────────

    @Transactional
    public void deactivateHoliday(Long id) {
        HolidayCalendar h = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found: " + id));
        h.setActive(false);
        repo.save(h);
        holidayChecker.invalidateCacheFor(h.getHolidayDate());
    }

    // ── HARD DELETE ───────────────────────────────────────────────

    @Transactional
    public void deleteHoliday(Long id) {
        HolidayCalendar h = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found: " + id));
        LocalDate date = h.getHolidayDate();
        repo.delete(h);
        holidayChecker.invalidateCacheFor(date);
    }
}