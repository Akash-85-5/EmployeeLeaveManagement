package com.example.employeeLeaveApplication.feature.announcement.repository;

import com.example.employeeLeaveApplication.feature.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Get all announcements for a team or global
    List<Announcement> findByIsGlobalTrueOrTeamId(Long teamId);
}