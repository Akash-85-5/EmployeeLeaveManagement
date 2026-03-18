package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {
    Optional<HolidayCalendar> findByHolidayDateAndActiveTrue(LocalDate date);

    @Query("""
        SELECT h FROM HolidayCalendar h
        WHERE  YEAR(h.holidayDate) = :year
          AND  h.active = true
        ORDER BY h.holidayDate ASC
    """)
    List<HolidayCalendar> findByYear(@Param("year") int year);

    @Query("""
        SELECT h FROM HolidayCalendar h
        WHERE  YEAR(h.holidayDate) = :year
        ORDER BY h.holidayDate ASC
    """)
    List<HolidayCalendar> findByYearAll(@Param("year") int year);

    boolean existsByHolidayDate(LocalDate date);
}
