package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.LeaveReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveReminderRepository extends JpaRepository<LeaveReminder, Long> {
    Optional<LeaveReminder> findByLeaveApplicationId(Long leaveApplicationId);
}
