package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.OvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OvertimeRepository extends JpaRepository<OvertimeRequest, Long> {
}
