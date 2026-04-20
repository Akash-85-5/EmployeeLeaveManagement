package com.emp_management.feature.vpn.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.feature.vpn.dto.VpnRequestDtos.*;
import com.emp_management.feature.vpn.entity.VpnRequest;
import com.emp_management.feature.vpn.repository.VpnRequestRepository;
import com.emp_management.shared.enums.Channel;
import com.emp_management.shared.enums.EventType;
import com.emp_management.shared.enums.VpnRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VpnRequestService {

    private final VpnRequestRepository vpnRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public VpnRequestService(VpnRequestRepository vpnRequestRepository,
                             EmployeeRepository employeeRepository,
                             NotificationService notificationService) {
        this.vpnRequestRepository = vpnRequestRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    // =========================================================
    // APPLY FOR VPN
    // =========================================================
    @Transactional
    public VpnRequestResponse applyForVpn(String applicantId, ApplyRequest request) {

        Employee applicant = findEmployeeOrThrow(applicantId);

        LocalDate today = LocalDate.now();

        if (request.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        String role = resolveRole(applicant);
        String managerApproverId = applicant.getReportingId();

        // A MANAGER with no reporting manager goes straight to ADMIN
        boolean isTopLevelManager =
                (managerApproverId == null && "MANAGER".equalsIgnoreCase(role));

        if (!isTopLevelManager && managerApproverId == null) {
            throw new IllegalStateException("No reporting manager is assigned to your profile. "
                    + "Please contact HR to update your reporting structure.");
        }

        VpnRequestStatus initialStatus = isTopLevelManager
                ? VpnRequestStatus.PENDING_ADMIN
                : VpnRequestStatus.PENDING_MANAGER;

        if (!isTopLevelManager) {
            validateManagerExists(managerApproverId);
        }

        VpnRequest vpnRequest = new VpnRequest();
        vpnRequest.setApplicantId(applicantId);
        vpnRequest.setApplicantRole(role);
        vpnRequest.setManagerApproverId(managerApproverId);
        vpnRequest.setPurpose(request.getPurpose());
        vpnRequest.setStatus(initialStatus);
        vpnRequest.setStartDate(request.getStartDate());
        vpnRequest.setEndDate(request.getEndDate());

        VpnRequest saved = vpnRequestRepository.save(vpnRequest);

        // Notify applicant — confirmation
        sendNotification(applicant, EventType.VPN_APPLIED,
                "Dear " + applicant.getName() + ",\n\n"
                        + "Your VPN access request has been successfully submitted and is currently "
                        + "awaiting approval.\n\n"
                        + "Request Summary:\n"
                        + "  Employee ID : " + applicant.getEmpId() + "\n"
                        + "  Name        : " + applicant.getName() + "\n"
                        + "  Purpose     : " + request.getPurpose() + "\n"
                        + "  Access From : " + request.getStartDate() + "\n"
                        + "  Access To   : " + request.getEndDate() + "\n\n"
                        + (isTopLevelManager
                        ? "Your request has been forwarded directly to the Administrator for review.\n"
                        : "Your request has been forwarded to your reporting manager for review.\n")
                        + "\nYou will be notified at each stage of the approval process.\n\n"
                        + "Regards,\nHR Management System");

        // Notify manager (if applicable)
        if (managerApproverId != null) {
            Employee manager = findEmployeeOrThrow(managerApproverId);
            sendNotification(manager, EventType.VPN_APPLIED,
                    "Dear " + manager.getName() + ",\n\n"
                            + "A VPN access request has been submitted by one of your direct reports "
                            + "and requires your review.\n\n"
                            + "Request Details:\n"
                            + "  Employee ID : " + applicant.getEmpId() + "\n"
                            + "  Name        : " + applicant.getName() + "\n"
                            + "  Purpose     : " + request.getPurpose() + "\n"
                            + "  Access From : " + request.getStartDate() + "\n"
                            + "  Access To   : " + request.getEndDate() + "\n\n"
                            + "Please log in to the HR portal to review and take action on this request.\n\n"
                            + "Regards,\nHR Management System");
        }

        // If top-level manager, notify all admins directly
        if (isTopLevelManager) {
            List<Employee> admins = employeeRepository.findAllByRoleName("ADMIN");
            for (Employee admin : admins) {
                sendNotification(admin, EventType.VPN_APPLIED,
                        "Dear " + admin.getName() + ",\n\n"
                                + "A VPN access request has been submitted by a Manager and requires "
                                + "your approval.\n\n"
                                + "Request Details:\n"
                                + "  Employee ID : " + applicant.getEmpId() + "\n"
                                + "  Name        : " + applicant.getName() + "\n"
                                + "  Role        : " + role + "\n"
                                + "  Purpose     : " + request.getPurpose() + "\n"
                                + "  Access From : " + request.getStartDate() + "\n"
                                + "  Access To   : " + request.getEndDate() + "\n\n"
                                + "Please log in to the HR portal to review and take action on this request.\n\n"
                                + "Regards,\nHR Management System");
            }
        }

        return toResponse(saved);
    }

    // =========================================================
    // MANAGER ACTION
    // =========================================================
    @Transactional
    public VpnRequestResponse managerAction(Long requestId, String managerId, ActionRequest action) {

        VpnRequest vpnRequest = findRequestOrThrow(requestId);
        Employee manager = findEmployeeOrThrow(managerId);
        Employee applicant = findEmployeeOrThrow(vpnRequest.getApplicantId());

        if (vpnRequest.getStatus() != VpnRequestStatus.PENDING_MANAGER) {
            throw new IllegalStateException("This request is not currently awaiting manager approval. "
                    + "Current status: " + vpnRequest.getStatus().name());
        }

        if (!managerId.equals(vpnRequest.getManagerApproverId())) {
            throw new SecurityException("You are not authorised to act on this request.");
        }

        if (managerId.equals(vpnRequest.getApplicantId())) {
            throw new IllegalStateException("You cannot approve or reject your own VPN request.");
        }

        vpnRequest.setManagerRemarks(action.getRemarks());
        vpnRequest.setManagerActionedAt(LocalDateTime.now());

        if (action.isApproved()) {
            vpnRequest.setStatus(VpnRequestStatus.PENDING_ADMIN);

            // Notify all admins
            List<Employee> admins = employeeRepository.findAllByRoleName("ADMIN");
            for (Employee admin : admins) {
                sendNotification(admin, EventType.VPN_MANAGER_APPROVED,
                        "Dear " + admin.getName() + ",\n\n"
                                + "A VPN access request has been approved by the applicant's reporting manager "
                                + "and now requires your final authorisation.\n\n"
                                + "Request Details:\n"
                                + "  Employee ID      : " + applicant.getEmpId() + "\n"
                                + "  Name             : " + applicant.getName() + "\n"
                                + "  Purpose          : " + vpnRequest.getPurpose() + "\n"
                                + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                                + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                                + "  Approved By      : " + manager.getName() + " (" + manager.getEmpId() + ")\n"
                                + "  Manager Remarks  : " + nullSafe(action.getRemarks()) + "\n\n"
                                + "Please log in to the HR portal to review and take action on this request.\n\n"
                                + "Regards,\nHR Management System");
            }

            // Notify applicant — manager approved
            sendNotification(applicant, EventType.VPN_MANAGER_APPROVED,
                    "Dear " + applicant.getName() + ",\n\n"
                            + "Your VPN access request has been approved by your reporting manager "
                            + "and is now pending final authorisation from the Administrator.\n\n"
                            + "Request Summary:\n"
                            + "  Employee ID      : " + applicant.getEmpId() + "\n"
                            + "  Name             : " + applicant.getName() + "\n"
                            + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                            + "  Status           : Manager Approved — Awaiting Admin Approval\n"
                            + "  Approved By      : " + manager.getName() + "\n"
                            + "  Manager Remarks  : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "You will be notified once the Administrator has reviewed your request.\n\n"
                            + "Regards,\nHR Management System");

            // Notify manager — confirmation of their own action
            sendNotification(manager, EventType.VPN_MANAGER_APPROVED,
                    "Dear " + manager.getName() + ",\n\n"
                            + "You have successfully approved the VPN access request for the following employee. "
                            + "The request has been forwarded to the Administrator for final authorisation.\n\n"
                            + "  Employee ID : " + applicant.getEmpId() + "\n"
                            + "  Name        : " + applicant.getName() + "\n"
                            + "  Access From : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To   : " + vpnRequest.getEndDate() + "\n\n"
                            + "Regards,\nHR Management System");

        } else {
            vpnRequest.setStatus(VpnRequestStatus.MANAGER_REJECTED);

            // Notify applicant — manager rejected
            sendNotification(applicant, EventType.VPN_REJECTED,
                    "Dear " + applicant.getName() + ",\n\n"
                            + "We regret to inform you that your VPN access request has been reviewed "
                            + "by your reporting manager and was not approved at this stage.\n\n"
                            + "Request Summary:\n"
                            + "  Employee ID      : " + applicant.getEmpId() + "\n"
                            + "  Name             : " + applicant.getName() + "\n"
                            + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                            + "  Status           : Rejected by Manager\n"
                            + "  Reviewed By      : " + manager.getName() + "\n"
                            + "  Remarks          : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "If you believe this decision requires further review, please contact your "
                            + "reporting manager or the HR department.\n\n"
                            + "Regards,\nHR Management System");

            // Notify manager — confirmation of their rejection
            sendNotification(manager, EventType.VPN_REJECTED,
                    "Dear " + manager.getName() + ",\n\n"
                            + "You have rejected the VPN access request for the following employee.\n\n"
                            + "  Employee ID : " + applicant.getEmpId() + "\n"
                            + "  Name        : " + applicant.getName() + "\n"
                            + "  Access From : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To   : " + vpnRequest.getEndDate() + "\n"
                            + "  Remarks     : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "Regards,\nHR Management System");
        }

        return toResponse(vpnRequestRepository.save(vpnRequest));
    }

    // =========================================================
    // ADMIN ACTION
    // =========================================================
    @Transactional
    public VpnRequestResponse adminAction(Long requestId, String adminId, ActionRequest action) {

        VpnRequest vpnRequest = findRequestOrThrow(requestId);
        Employee admin = findEmployeeOrThrow(adminId);
        Employee applicant = findEmployeeOrThrow(vpnRequest.getApplicantId());

        // FIX: Once one admin acts, status is no longer PENDING_ADMIN.
        // Other admins calling this endpoint will correctly get this error.
        if (vpnRequest.getStatus() != VpnRequestStatus.PENDING_ADMIN) {
            throw new IllegalStateException("This request is no longer awaiting admin approval. "
                    + "Current status: " + vpnRequest.getStatus().name());
        }

        if (adminId.equals(vpnRequest.getApplicantId())) {
            throw new IllegalStateException("You cannot approve or reject your own VPN request.");
        }

        vpnRequest.setAdminId(adminId);
        vpnRequest.setAdminRemarks(action.getRemarks());
        vpnRequest.setAdminActionedAt(LocalDateTime.now());

        // Resolve manager name for context (may be null for top-level managers)
        String managerInfo = vpnRequest.getManagerApproverId() != null
                ? employeeRepository.findByEmpId(vpnRequest.getManagerApproverId())
                .map(m -> m.getName() + " (" + m.getEmpId() + ")")
                .orElse(vpnRequest.getManagerApproverId())
                : "N/A (Direct submission)";

        if (action.isApproved()) {
            vpnRequest.setStatus(VpnRequestStatus.APPROVED);

            // Notify applicant — fully approved
            sendNotification(applicant, EventType.VPN_APPROVED,
                    "Dear " + applicant.getName() + ",\n\n"
                            + "We are pleased to inform you that your VPN access request has been fully "
                            + "approved by the Administrator. You may now proceed with the VPN setup.\n\n"
                            + "Request Summary:\n"
                            + "  Employee ID      : " + applicant.getEmpId() + "\n"
                            + "  Name             : " + applicant.getName() + "\n"
                            + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                            + "  Status           : Approved\n"
                            + "  Approved By      : " + admin.getName() + " (Administrator)\n"
                            + "  Admin Remarks    : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "Please contact your IT department if you require assistance with VPN configuration.\n\n"
                            + "Regards,\nHR Management System");

            // Notify admin — confirmation of their approval
            sendNotification(admin, EventType.VPN_APPROVED,
                    "Dear " + admin.getName() + ",\n\n"
                            + "You have successfully approved the VPN access request for the following employee.\n\n"
                            + "  Employee ID      : " + applicant.getEmpId() + "\n"
                            + "  Name             : " + applicant.getName() + "\n"
                            + "  Role             : " + vpnRequest.getApplicantRole() + "\n"
                            + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                            + "  Manager Approved : " + managerInfo + "\n\n"
                            + "Regards,\nHR Management System");

        } else {
            vpnRequest.setStatus(VpnRequestStatus.ADMIN_REJECTED);

            // Notify applicant — admin rejected
            sendNotification(applicant, EventType.VPN_REJECTED,
                    "Dear " + applicant.getName() + ",\n\n"
                            + "We regret to inform you that your VPN access request has been reviewed "
                            + "by the Administrator and was not approved.\n\n"
                            + "Request Summary:\n"
                            + "  Employee ID      : " + applicant.getEmpId() + "\n"
                            + "  Name             : " + applicant.getName() + "\n"
                            + "  Access From      : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To        : " + vpnRequest.getEndDate() + "\n"
                            + "  Status           : Rejected by Administrator\n"
                            + "  Reviewed By      : " + admin.getName() + "\n"
                            + "  Remarks          : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "For further clarification, please reach out to the HR department.\n\n"
                            + "Regards,\nHR Management System");

            // Notify admin — confirmation of their rejection
            sendNotification(admin, EventType.VPN_REJECTED,
                    "Dear " + admin.getName() + ",\n\n"
                            + "You have rejected the VPN access request for the following employee.\n\n"
                            + "  Employee ID : " + applicant.getEmpId() + "\n"
                            + "  Name        : " + applicant.getName() + "\n"
                            + "  Access From : " + vpnRequest.getStartDate() + "\n"
                            + "  Access To   : " + vpnRequest.getEndDate() + "\n"
                            + "  Remarks     : " + nullSafe(action.getRemarks()) + "\n\n"
                            + "Regards,\nHR Management System");
        }

        return toResponse(vpnRequestRepository.save(vpnRequest));
    }

    // =========================================================
    // FETCH — applicant tracking
    // =========================================================

    public VpnRequestResponse getRequestById(Long requestId, String requesterId) {
        VpnRequest vpnRequest = findRequestOrThrow(requestId);

        // Only the applicant themselves, their manager, or an admin may view a specific request
        boolean isApplicant = requesterId.equals(vpnRequest.getApplicantId());
        boolean isManager   = requesterId.equals(vpnRequest.getManagerApproverId());
        boolean isAdmin     = employeeRepository.findByEmpId(requesterId)
                .map(e -> "ADMIN".equalsIgnoreCase(resolveRole(e)))
                .orElse(false);

        if (!isApplicant && !isManager && !isAdmin) {
            throw new SecurityException("You are not authorised to view this request.");
        }

        return toResponse(vpnRequest);
    }

    public List<VpnRequestResponse> getMyRequests(String applicantId) {
        // 1. Log the ID to see exactly what Spring Security is giving you
        System.out.println("Searching for Applicant ID: [" + applicantId + "]");

        List<VpnRequest> results = vpnRequestRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);

        // 2. See if the DB returns anything at all regardless of the ID
        // System.out.println("Total records in DB: " + vpnRequestRepository.count());

        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VpnRequestResponse> getPendingForManager(String managerId) {
        return vpnRequestRepository
                .findByManagerApproverIdAndStatusOrderByCreatedAtDesc(
                        managerId, VpnRequestStatus.PENDING_MANAGER)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VpnRequestResponse> getAllForManager(String managerId) {
        return vpnRequestRepository
                .findByManagerApproverIdOrderByCreatedAtDesc(managerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // FIX: Returns only PENDING_ADMIN requests.
    // Multi-admin safety: once any admin acts, status changes and request
    // disappears from every other admin's pending list automatically.
    public List<VpnRequestResponse> getPendingForAdmin() {
        return vpnRequestRepository
                .findByStatusOrderByCreatedAtDesc(VpnRequestStatus.PENDING_ADMIN)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VpnRequestResponse> getAllRequests() {
        return vpnRequestRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================
    private void sendNotification(Employee user, EventType eventType, String messageBody) {
        String email = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : "fallback@email.com";

        // Derive a clean one-line subject from the eventType
        String subject = resolveEmailSubject(eventType);

        // In-app notification — uses the new overload, stores the full message body
        notificationService.createNotification(
                user.getEmpId(),
                "system@hrms.com",
                email,
                eventType,
                Channel.IN_APP,
                subject,
                messageBody
        );

        // Email notification — same overload, failures don't roll back the transaction
        try {
            notificationService.createNotification(
                    user.getEmpId(),
                    "system@hrms.com",
                    email,
                    eventType,
                    Channel.EMAIL,
                    subject,
                    messageBody
            );
        } catch (Exception e) {
            System.err.println("[VPN] Email notification failed for "
                    + user.getEmpId() + " — " + e.getMessage());
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private Employee findEmployeeOrThrow(String id) {
        return employeeRepository.findByEmpId(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    private VpnRequest findRequestOrThrow(Long id) {
        return vpnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VPN request not found: " + id));
    }

    private void validateManagerExists(String managerId) {
        if (managerId == null) return;
        if (employeeRepository.findByEmpId(managerId).isEmpty()) {
            throw new IllegalArgumentException("Assigned reporting manager not found: " + managerId);
        }
    }

    private String resolveRole(Employee employee) {
        return (employee.getRole() != null)
                ? employee.getRole().getRoleName()
                : "EMPLOYEE";
    }

    // Returns "—" instead of "null" in notification messages
    private String nullSafe(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }

    private VpnRequestResponse toResponse(VpnRequest r) {
        VpnRequestResponse res = new VpnRequestResponse();

        res.setId(r.getId());
        res.setApplicantId(r.getApplicantId());
        res.setApplicantName(
                employeeRepository.findByEmpId(r.getApplicantId())
                        .map(Employee::getName).orElse(null));
        res.setApplicantRole(r.getApplicantRole());
        res.setManagerApproverId(r.getManagerApproverId());

        if (r.getManagerApproverId() != null) {
            res.setManagerApproverName(
                    employeeRepository.findByEmpId(r.getManagerApproverId())
                            .map(Employee::getName).orElse(null));
        }

        if (r.getAdminId() != null) {
            res.setAdminName(
                    employeeRepository.findByEmpId(r.getAdminId())
                            .map(Employee::getName).orElse(null));
        }

        res.setPurpose(r.getPurpose());
        res.setStatus(r.getStatus());
        res.setStatusLabel(resolveStatusLabel(r.getStatus()));  // FIX: human-readable label

        res.setManagerRemarks(r.getManagerRemarks());
        res.setManagerActionedAt(r.getManagerActionedAt());

        res.setAdminId(r.getAdminId());
        res.setAdminRemarks(r.getAdminRemarks());
        res.setAdminActionedAt(r.getAdminActionedAt());

        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        res.setStartDate(r.getStartDate());
        res.setEndDate(r.getEndDate());

        return res;
    }

    // FIX: Meaningful status labels for the applicant's tracking view
    private String resolveStatusLabel(VpnRequestStatus status) {
        return switch (status) {
            case PENDING_MANAGER  -> "Pending Manager Approval";
            case PENDING_ADMIN    -> "Manager Approved — Awaiting Admin Approval";
            case APPROVED         -> "Approved";
            case MANAGER_REJECTED -> "Rejected by Manager";
            case ADMIN_REJECTED   -> "Rejected by Administrator";
        };
    }
    private String resolveEmailSubject(EventType eventType) {
        return switch (eventType) {
            case VPN_APPLIED          -> "[HR Portal] VPN Access Request — Submitted";
            case VPN_MANAGER_APPROVED -> "[HR Portal] VPN Access Request — Manager Approved";
            case VPN_APPROVED         -> "[HR Portal] VPN Access Request — Approved";
            case VPN_REJECTED         -> "[HR Portal] VPN Access Request — Rejected";
            default                   -> "[HR Portal] VPN Access Request — Update";
        };
    }
}