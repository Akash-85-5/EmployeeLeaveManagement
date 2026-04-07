package com.emp_management.feature.holiday.service;

import com.emp_management.feature.holiday.entity.HolidayCalendar;
import com.emp_management.feature.holiday.repository.HolidayCalendarRepository;
import com.emp_management.feature.holiday.utils.HolidayChecker;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
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
    private final HolidayChecker holidayChecker;

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
            throw new BadRequestException(
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
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found: " + id));

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
                .orElseThrow(() -> new BadRequestException("Holiday not found: " + id));
        h.setActive(false);
        repo.save(h);
        holidayChecker.invalidateCacheFor(h.getHolidayDate());
    }

    // ── HARD DELETE ───────────────────────────────────────────────

    @Transactional
    public void deleteHoliday(Long id) {
        HolidayCalendar h = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found: " + id));
        LocalDate date = h.getHolidayDate();
        repo.delete(h);
        holidayChecker.invalidateCacheFor(date);
    }
}