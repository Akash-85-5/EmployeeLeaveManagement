package com.example.employeeLeaveApplication.feature.news.repository;

import com.example.employeeLeaveApplication.feature.news.entity.FlashNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashNewsRepository extends JpaRepository<FlashNews, Long> {

    List<FlashNews> findByActiveTrueAndExpiryDateAfterOrderByCreatedAtDesc(LocalDateTime now);
    List<FlashNews> findByActiveTrueAndExpiryDateAfterOrderByPriorityDescCreatedAtDesc(LocalDateTime now);
}