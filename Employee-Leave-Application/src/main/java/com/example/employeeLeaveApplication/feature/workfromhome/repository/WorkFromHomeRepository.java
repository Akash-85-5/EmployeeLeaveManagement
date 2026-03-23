package com.example.employeeLeaveApplication.feature.workfromhome.repository;

import com.example.employeeLeaveApplication.feature.workfromhome.entity.WorkFromHome;
import com.example.employeeLeaveApplication.shared.enums.WfhStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkFromHomeRepository extends JpaRepository<WorkFromHome, Long> {

    List<WorkFromHome> findByEmployeeId(Long employeeId);

    @Query("""
    SELECT w FROM WorkFromHome w
    WHERE w.employeeId = :employeeId
    AND :date BETWEEN w.startDate AND w.endDate
    AND w.status = :status
""")
    Optional<WorkFromHome> findApprovedWfhForEmployeeOnDate(
            Long employeeId,
            LocalDate date,
            WfhStatus status
    );

}