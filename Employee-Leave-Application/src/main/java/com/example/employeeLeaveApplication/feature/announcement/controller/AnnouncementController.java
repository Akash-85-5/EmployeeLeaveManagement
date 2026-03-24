package com.example.employeeLeaveApplication.feature.announcement.controller;

import com.example.employeeLeaveApplication.feature.announcement.entity.Announcement;
import com.example.employeeLeaveApplication.feature.announcement.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService){
        this.announcementService = announcementService;
    }

    // ✅ Employee view announcements
    @GetMapping("/employee/{empId}")
    public List<Announcement> getEmployeeAnnouncements(@PathVariable Long empId){
        return announcementService.getAnnouncementsForEmployee(empId);
    }
}