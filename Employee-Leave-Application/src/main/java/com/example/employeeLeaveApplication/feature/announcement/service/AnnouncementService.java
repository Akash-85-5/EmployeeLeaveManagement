package com.example.employeeLeaveApplication.feature.announcement.service;

import com.example.employeeLeaveApplication.feature.announcement.dto.AnnouncementRequest;
import com.example.employeeLeaveApplication.feature.announcement.entity.Announcement;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.shared.enums.Channel;
import com.example.employeeLeaveApplication.shared.enums.EventType;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.feature.announcement.repository.AnnouncementRepository;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final EmployeeRepository employeeRepo;
    private final NotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepo,
                               EmployeeRepository employeeRepo,
                               NotificationService notificationService){
        this.announcementRepo = announcementRepo;
        this.employeeRepo = employeeRepo;
        this.notificationService = notificationService;
    }

    // Create announcement (admin only)
    public Announcement createAnnouncement(String adminEmail, AnnouncementRequest req){

        Employee admin = employeeRepo.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if(admin.getRole() != Role.ADMIN){
            throw new RuntimeException("Only Admin can create announcement");
        }

        Announcement ann = new Announcement();
        ann.setTitle(req.getTitle());
        ann.setMessage(req.getMessage());
        ann.setCreatedBy(adminEmail);
        ann.setCreatedAt(LocalDateTime.now());
        ann.setGlobal(req.isGlobal());
        ann.setTeamId(req.getTeamId());
        ann.setReplacementName(req.getReplacementName());

        Announcement saved = announcementRepo.save(ann);

        // ✅ Notify employees
        List<Employee> employees;
        if(req.isGlobal()){
            employees = employeeRepo.findAll();
        } else {
            employees = employeeRepo.findByTeamId(req.getTeamId());
        }

        String message = req.getMessage();
        if(req.getReplacementName() != null){
            message += " | Replacement: " + req.getReplacementName();
        }

        for (Employee emp : employees) {

            String context = req.getTitle() + " - " + message;

            notificationService.createNotification(
                    emp.getId(),                  // userId
                    adminEmail,                  // fromEmail
                    emp.getEmail(),              // toEmail
                    EventType.ANNOUNCEMENT,      // eventType
                    emp.getRole(),               // recipientType
                    Channel.IN_APP,              // or EMAIL
                    context                      // message content
            );
        }

        return saved;
    }

    // Get all announcements (for admin)
    public List<Announcement> getAllAnnouncements(){
        return announcementRepo.findAll();
    }

    // Get announcements for a specific employee (filtered by team/global)
    public List<Announcement> getAnnouncementsForEmployee(Long empId){
        Employee emp = employeeRepo.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        return announcementRepo.findByIsGlobalTrueOrTeamId(emp.getTeamId());
    }
}