package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LossOfPayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LossOfPayRecordRepository
        extends JpaRepository<LossOfPayRecord, Long> {

    Optional<LossOfPayRecord>
    findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    /**
     * YEARLY LOP = SUM OF MONTHLY LOP
     * Comp-off is NOT involved
     */
//    @Query("""
//        SELECT COALESCE(SUM(l.monthlyViolationLop), 0.0)
//        FROM LossOfPayRecord l
//        WHERE l.employeeId = :empId
//        AND l.year = :year
//    """)
//    Double getTotalLopForYear(
//            @Param("empId") Long empId,
//            @Param("year") Integer year
//    );
}
