package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.FlashNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashNewsRepository extends JpaRepository<FlashNews, Long> {

    List<FlashNews> findByActiveTrueAndExpiryDateAfterOrderByCreatedAtDesc(LocalDateTime now);
    List<FlashNews> findByActiveTrueAndExpiryDateAfterOrderByPriorityDescCreatedAtDesc(LocalDateTime now);
}