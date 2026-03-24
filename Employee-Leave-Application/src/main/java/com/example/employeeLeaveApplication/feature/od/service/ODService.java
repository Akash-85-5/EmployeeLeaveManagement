package com.example.employeeLeaveApplication.feature.od.service;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.od.entity.ODRequest;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.shared.exceptions.ResourceNotFoundException;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.od.repository.ODRequestRepository;
import com.example.employeeLeaveApplication.shared.enums.Channel;
import com.example.employeeLeaveApplication.shared.enums.EventType;
import com.example.employeeLeaveApplication.shared.enums.ODStatus;
import com.example.employeeLeaveApplication.shared.enums.Role;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ODService {

    private final ODRequestRepository odRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public ODService(ODRequestRepository odRepository,
                     EmployeeRepository employeeRepository,
                     NotificationService notificationService) {
        this.odRepository = odRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    public ODRequest createOD(Long employeeId, ODRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        List<ODRequest> overlappingODs = odRepository.findOverlappingODs(
                employeeId,
                request.getStartDate(),
                request.getEndDate()
        );
        if (!overlappingODs.isEmpty()) {
            throw new BadRequestException(
                    "You already have an active OD request that overlaps with these dates.");
        }

        request.setEmployeeId(employeeId);

        switch (employee.getRole()) {
            case EMPLOYEE ->    request.setStatus(ODStatus.PENDING_TEAM_LEADER);
            case TEAM_LEADER -> request.setStatus(ODStatus.PENDING_MANAGER);
            case MANAGER,
                 ADMIN ->       request.setStatus(ODStatus.PENDING_HR);
            default -> throw new BadRequestException("HR cannot create OD request");
        }

        request.setEmployeeName(employee.getName());
        ODRequest saved = odRepository.save(request);

        notifyFirstApprover(saved, employee);

        return saved;
    }

    public ODRequest approveOD(Long odId, Long approverId) {

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        switch (od.getStatus()) {

            case PENDING_TEAM_LEADER -> {
                if (approver.getRole() != Role.TEAM_LEADER)
                    throw new BadRequestException("Only Team Leader can approve");

                od.setStatus(ODStatus.PENDING_MANAGER);
                odRepository.save(od);

                notifyEmployeeProgress(od, employee, approver,
                        "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                                + " has been approved by Team Leader. Pending Manager approval.");

                notifyNextApprover(od, employee, approver, ODStatus.PENDING_MANAGER);
            }

            case PENDING_MANAGER -> {
                if (approver.getRole() != Role.MANAGER)
                    throw new BadRequestException("Only Manager can approve");

                od.setStatus(ODStatus.APPROVED);
                odRepository.save(od);

                notifyEmployeeFinal(od, employee, approver, true, null);
            }

            case PENDING_HR -> {
                if (approver.getRole() != Role.HR)
                    throw new BadRequestException("Only HR can approve");

                od.setStatus(ODStatus.APPROVED);
                odRepository.save(od);

                notifyEmployeeFinal(od, employee, approver, true, null);
            }

            default -> throw new BadRequestException("OD already processed");
        }

        return od;
    }

    public ODRequest rejectOD(Long odId, Long approverId, String reason) {

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (od.getStatus() == ODStatus.APPROVED ||
                od.getStatus() == ODStatus.REJECTED ||
                od.getStatus() == ODStatus.CANCELLED) {
            throw new BadRequestException("OD already finalized");
        }

        od.setStatus(ODStatus.REJECTED);
        odRepository.save(od);

        notifyEmployeeFinal(od, employee, approver, false, reason);

        return od;
    }

    public ODRequest cancelOD(Long odId, Long userId) {

        Employee user = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (od.getStatus() == ODStatus.REJECTED ||
                od.getStatus() == ODStatus.CANCELLED) {
            throw new BadRequestException("OD already finalized");
        }

        if (od.getStatus() == ODStatus.APPROVED) {
            if (user.getRole() != Role.HR) {
                throw new BadRequestException("Only HR can cancel approved OD");
            }
            od.setStatus(ODStatus.CANCELLED);
            odRepository.save(od);
            notifyEmployeeCancel(od, employee, user);
            return od;
        }

        if (od.getEmployeeId().equals(userId)) {
            od.setStatus(ODStatus.CANCELLED);
            odRepository.save(od);
            return od;
        }

        boolean allowed =
                (user.getRole() == Role.TEAM_LEADER &&
                        od.getStatus() == ODStatus.PENDING_TEAM_LEADER) ||
                        (user.getRole() == Role.MANAGER &&
                                od.getStatus() == ODStatus.PENDING_MANAGER) ||
                        (user.getRole() == Role.HR &&
                                od.getStatus() == ODStatus.PENDING_HR);

        if (!allowed) {
            throw new BadRequestException("You are not allowed to cancel this OD request");
        }

        od.setStatus(ODStatus.CANCELLED);
        odRepository.save(od);

        notifyEmployeeCancel(od, employee, user);

        return od;
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void notifyFirstApprover(ODRequest od, Employee employee) {

        String context = employee.getName() + " has submitted an OD request from "
                + od.getStartDate() + " to " + od.getEndDate()
                + ". Please review and approve.";

        switch (od.getStatus()) {

            case PENDING_TEAM_LEADER -> {
                if (employee.getTeamLeaderId() == null) return;
                Employee tl = employeeRepository
                        .findById(employee.getTeamLeaderId()).orElse(null);
                if (tl == null) return;

                notificationService.createNotification(
                        tl.getId(),
                        employee.getEmail(),
                        tl.getEmail(),
                        EventType.OD_APPLIED,
                        tl.getRole(),
                        Channel.EMAIL,
                        context
                );
            }

            case PENDING_MANAGER -> {
                if (employee.getManagerId() == null) return;
                Employee manager = employeeRepository
                        .findById(employee.getManagerId()).orElse(null);
                if (manager == null) return;

                notificationService.createNotification(
                        manager.getId(),
                        employee.getEmail(),
                        manager.getEmail(),
                        EventType.OD_APPLIED,
                        manager.getRole(),
                        Channel.EMAIL,
                        context
                );
            }

            case PENDING_HR -> {
                List<Employee> hrList = employeeRepository.findAllHr();
                for (Employee hr : hrList) {
                    notificationService.createNotification(
                            hr.getId(),
                            employee.getEmail(),
                            hr.getEmail(),
                            EventType.OD_APPLIED,
                            hr.getRole(),
                            Channel.EMAIL,
                            context
                    );
                }
            }
        }
    }

    private void notifyNextApprover(ODRequest od, Employee employee,
                                    Employee currentApprover, ODStatus nextStatus) {
        if (nextStatus != ODStatus.PENDING_MANAGER) return;

        if (employee.getManagerId() == null) return;
        Employee manager = employeeRepository
                .findById(employee.getManagerId()).orElse(null);
        if (manager == null) return;

        notificationService.createNotification(
                manager.getId(),
                currentApprover.getEmail(),
                manager.getEmail(),
                EventType.OD_APPLIED,
                manager.getRole(),
                Channel.EMAIL,
                employee.getName() + "'s OD request from "
                        + od.getStartDate() + " to " + od.getEndDate()
                        + " has been approved by Team Leader. Awaiting your approval."
        );
    }

    private void notifyEmployeeProgress(ODRequest od, Employee employee,
                                        Employee approver, String message) {
        notificationService.createNotification(
                employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                EventType.OD_IN_PROGRESS,
                employee.getRole(),
                Channel.EMAIL,
                message
        );
    }

    private void notifyEmployeeFinal(ODRequest od, Employee employee,
                                     Employee approver, boolean approved,
                                     String reason) {
        String context;
        EventType eventType;

        if (approved) {
            context = "Your OD request from " + od.getStartDate()
                    + " to " + od.getEndDate() + " has been fully approved.";
            eventType = EventType.OD_APPROVED;
        } else {
            context = "Your OD request from " + od.getStartDate()
                    + " to " + od.getEndDate() + " has been rejected."
                    + (reason != null ? " Reason: " + reason : "");
            eventType = EventType.OD_REJECTED;
        }

        notificationService.createNotification(
                employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                eventType,
                employee.getRole(),
                Channel.EMAIL,
                context
        );
    }

    private void notifyEmployeeCancel(ODRequest od, Employee employee, Employee cancelledBy) {
        notificationService.createNotification(
                employee.getId(),
                cancelledBy.getEmail(),
                employee.getEmail(),
                EventType.OD_CANCELLED,
                employee.getRole(),
                Channel.EMAIL,
                "Your OD request from " + od.getStartDate()
                        + " to " + od.getEndDate()
                        + " has been cancelled by " + cancelledBy.getName() + "."
        );
    }

    public List<ODRequest> getMyODRequests(Long employeeId) {
        return odRepository.findByEmployeeId(employeeId);
    }

    public List<ODRequest> getPendingForTeamLeader(Long tlId) {
        return odRepository.findPendingForTeamLeader(tlId);
    }

    public List<ODRequest> getPendingForHR() {
        return odRepository.findByStatus(ODStatus.PENDING_HR);
    }
    public List<ODRequest> getPendingForManager(Long managerId) {
        return odRepository.findPendingForManager(managerId);
    }
}