package com.example.employeeLeaveApplication.feature.overtime.repository;

import com.example.employeeLeaveApplication.feature.overtime.entity.OvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OvertimeRepository extends JpaRepository<OvertimeRequest, Long> {
}
