package com.emp_management.feature.holiday.controller;


import com.emp_management.feature.holiday.entity.HolidayCalendar;
import com.emp_management.feature.holiday.service.HolidayCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/holidays")
@PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
public class HolidayAdminController {

    private final HolidayCalendarService holidayService;

    public HolidayAdminController(HolidayCalendarService holidayService) {
        this.holidayService = holidayService;
    }

    // GET all active holidays for a year
    @GetMapping("/year/{year}")
    public ResponseEntity<List<HolidayCalendar>> getHolidaysForYear(@PathVariable int year) {
        List<HolidayCalendar> holidays = holidayService.getHolidaysForYear(year);
        return ResponseEntity.ok(holidays);
    }

    // GET all holidays (including inactive) for admin management
    @GetMapping("/year/{year}/all")
    public ResponseEntity<List<HolidayCalendar>> getAllHolidaysForYear(@PathVariable int year) {
        List<HolidayCalendar> holidays = holidayService.getAllHolidaysForYear(year);
        return ResponseEntity.ok(holidays);
    }

    // POST add a new holiday
    @PostMapping
    public ResponseEntity<HolidayCalendar> addHoliday(@RequestBody HolidayRequest request) {
        HolidayCalendar saved = holidayService.addHoliday(
                request.getHolidayDate(),
                request.getHolidayName(),
                request.getHolidayType(),
                1L  // Replace with actual logged-in user ID
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT update a holiday
    @PutMapping("/{id}")
    public ResponseEntity<HolidayCalendar> updateHoliday(
            @PathVariable Long id,
            @RequestBody HolidayRequest request) {
        HolidayCalendar updated = holidayService.updateHoliday(
                id,
                request.getHolidayName(),
                request.getHolidayType(),
                request.isActive()
        );
        return ResponseEntity.ok(updated);
    }

    // DELETE (hard delete) a holiday
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE (soft delete) — just mark as inactive
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateHoliday(@PathVariable Long id) {
        holidayService.deactivateHoliday(id);
        return ResponseEntity.noContent().build();
    }

    // DTO for requests
    public static class HolidayRequest {
        private LocalDate holidayDate;
        private String holidayName;
        private String holidayType = "NATIONAL";
        private boolean active = true;

        // Getters and Setters
        public LocalDate getHolidayDate() { return holidayDate; }
        public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

        public String getHolidayName() { return holidayName; }
        public void setHolidayName(String holidayName) { this.holidayName = holidayName; }

        public String getHolidayType() { return holidayType; }
        public void setHolidayType(String holidayType) { this.holidayType = holidayType; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}