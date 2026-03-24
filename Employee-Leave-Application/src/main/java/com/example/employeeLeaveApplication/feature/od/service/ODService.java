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

    // ───────────────────────────────────────────────
    // CREATE
    // ───────────────────────────────────────────────
    public ODRequest createOD(Long employeeId, ODRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // HR role cannot apply OD
        if (employee.getRole() == Role.HR) {
            throw new BadRequestException("HR cannot create an OD request");
        }

        // Employee must have a manager to route the request
        if (employee.getReportingId() == null) {
            throw new BadRequestException("No manager assigned; cannot submit OD request");
        }

        // Overlap check
        List<ODRequest> overlapping = odRepository.findOverlappingODs(
                employeeId, request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BadRequestException(
                    "You already have an active OD request that overlaps with these dates.");
        }

        Employee level1Manager = employeeRepository.findById(employee.getReportingId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        request.setEmployeeId(employeeId);
        request.setEmployeeName(employee.getName());
        request.setStatus(ODStatus.PENDING);
        request.setCurrentApproverId(level1Manager.getId());
        request.setApprovalLevel(1);

        ODRequest saved = odRepository.save(request);

        // Notify level-1 manager
        notify(level1Manager.getId(),
                employee.getEmail(),
                level1Manager.getEmail(),
                EventType.OD_APPLIED,
                level1Manager.getRole(),
                employee.getName() + " submitted an OD request from "
                        + saved.getStartDate() + " to " + saved.getEndDate()
                        + ". Please review and approve.");

        return saved;
    }

    // ───────────────────────────────────────────────
    // APPROVE
    // ───────────────────────────────────────────────
    public ODRequest approveOD(Long odId, Long approverId) {

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        if (od.getStatus() != ODStatus.PENDING) {
            throw new BadRequestException("OD is already finalized");
        }

        // Only the designated current approver can approve
        if (!od.getCurrentApproverId().equals(approverId)) {
            throw new BadRequestException("You are not the designated approver for this OD");
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (od.getApprovalLevel() == 1) {
            // Check if level-1 manager has a manager above them (level-2)
            if (approver.getReportingId() != null) {
                Employee level2Manager = employeeRepository.findById(approver.getReportingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Level-2 manager not found"));

                od.setApprovalLevel(2);
                od.setCurrentApproverId(level2Manager.getId());
                odRepository.save(od);

                // Notify employee of progress
                notify(employee.getId(),
                        approver.getEmail(),
                        employee.getEmail(),
                        EventType.OD_IN_PROGRESS,
                        employee.getRole(),
                        "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                                + " was approved by " + approver.getName()
                                + " and is now pending final approval.");

                // Notify level-2 manager
                notify(level2Manager.getId(),
                        approver.getEmail(),
                        level2Manager.getEmail(),
                        EventType.OD_APPLIED,
                        level2Manager.getRole(),
                        employee.getName() + "'s OD request from "
                                + od.getStartDate() + " to " + od.getEndDate()
                                + " has been approved by " + approver.getName()
                                + ". Awaiting your final approval.");

            } else {
                // No level-2 manager → single approval is enough
                finalizeApproval(od, employee, approver);
            }

        } else if (od.getApprovalLevel() == 2) {
            finalizeApproval(od, employee, approver);

        } else {
            throw new BadRequestException("Unexpected approval level");
        }

        return od;
    }

    // ───────────────────────────────────────────────
    // REJECT
    // ───────────────────────────────────────────────
    public ODRequest rejectOD(Long odId, Long approverId, String reason) {

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        if (od.getStatus() != ODStatus.PENDING) {
            throw new BadRequestException("OD is already finalized");
        }

        if (!od.getCurrentApproverId().equals(approverId)) {
            throw new BadRequestException("You are not the designated approver for this OD");
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        od.setStatus(ODStatus.REJECTED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        notify(employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                EventType.OD_REJECTED,
                employee.getRole(),
                "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                        + " has been rejected by " + approver.getName()
                        + (reason != null ? ". Reason: " + reason : "."));

        return od;
    }

    // ───────────────────────────────────────────────
    // CANCEL
    // ───────────────────────────────────────────────
    public ODRequest cancelOD(Long odId, Long userId) {

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        Employee user = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Employee employee = employeeRepository.findById(od.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (od.getStatus() == ODStatus.REJECTED || od.getStatus() == ODStatus.CANCELLED) {
            throw new BadRequestException("OD is already finalized");
        }

        // Approved OD can only be cancelled by HR
        if (od.getStatus() == ODStatus.APPROVED) {
            if (user.getRole() != Role.HR) {
                throw new BadRequestException("Only HR can cancel an approved OD");
            }
            return doCancel(od, employee, user);
        }

        // PENDING: employee cancels their own, or the current approver cancels it
        boolean isOwner = od.getEmployeeId().equals(userId);
        boolean isCurrentApprover = od.getCurrentApproverId().equals(userId);

        if (!isOwner && !isCurrentApprover) {
            throw new BadRequestException("You are not allowed to cancel this OD request");
        }

        return doCancel(od, employee, user);
    }

    // ───────────────────────────────────────────────
    // QUERIES
    // ───────────────────────────────────────────────

    public List<ODRequest> getMyODRequests(Long employeeId) {
        return odRepository.findByEmployeeId(employeeId);
    }

    /** Returns all PENDING ODs currently waiting on this manager */
    public List<ODRequest> getPendingForApprover(Long managerId) {
        return odRepository.findByCurrentApproverIdAndStatus(managerId, ODStatus.PENDING);
    }

    public List<ODRequest> getAllPendingODs() {
        return odRepository.findByStatus(ODStatus.PENDING);
    }

    // ───────────────────────────────────────────────
    // PRIVATE HELPERS
    // ───────────────────────────────────────────────

    private void finalizeApproval(ODRequest od, Employee employee, Employee approver) {
        od.setStatus(ODStatus.APPROVED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        notify(employee.getId(),
                approver.getEmail(),
                employee.getEmail(),
                EventType.OD_APPROVED,
                employee.getRole(),
                "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                        + " has been fully approved.");
    }

    private ODRequest doCancel(ODRequest od, Employee employee, Employee cancelledBy) {
        od.setStatus(ODStatus.CANCELLED);
        od.setCurrentApproverId(null);
        odRepository.save(od);

        // Don't notify if the employee cancelled their own
        if (!cancelledBy.getId().equals(employee.getId())) {
            notify(employee.getId(),
                    cancelledBy.getEmail(),
                    employee.getEmail(),
                    EventType.OD_CANCELLED,
                    employee.getRole(),
                    "Your OD request from " + od.getStartDate() + " to " + od.getEndDate()
                            + " has been cancelled by " + cancelledBy.getName() + ".");
        }

        return od;
    }

    private void notify(Long recipientId, String fromEmail, String toEmail,
                        EventType eventType, Role role, String context) {
        notificationService.createNotification(
                recipientId, fromEmail, toEmail, eventType, role, Channel.EMAIL, context);
    }
}