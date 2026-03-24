package com.example.employeeLeaveApplication.feature.access.service;

import com.example.employeeLeaveApplication.feature.access.entity.AccessRequest;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.access.dto.*;
import com.example.employeeLeaveApplication.feature.notification.service.NotificationService;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.feature.access.repository.AccessRequestRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.auth.repository.UserRepository;
import com.example.employeeLeaveApplication.shared.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Access Requests (VPN & Biometric)
 * <p>
 * Flow:
 * 1. Employee submits request (status: SUBMITTED)
 * 2. Manager approves/rejects (status: MANAGER_APPROVED/MANAGER_REJECTED)
 * 3. If manager approved, admin approves/rejects (status: ADMIN_APPROVED/ADMIN_REJECTED)
 * 4. If admin approved, employee gets actual access in Employee table
 */
@Service
@Slf4j
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final NotificationService notificationService;

    public AccessRequestService(
            AccessRequestRepository accessRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmployeePersonalDetailsRepository personalDetailsRepository,
            NotificationService notificationService) {
        this.accessRequestRepository = accessRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
        this.notificationService = notificationService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMPLOYEE ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Employee submits a new access request
     * - Only one active request per access type allowed
     * - Sets manager ID from employee's manager relationship
     */
    @Transactional
    public AccessRequestResponseDto submitAccessRequest(
            Long employeeId,
            SubmitAccessRequestDto request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        if (employee.getReportingId() == null) {
            throw new BadRequestException("No manager assigned to your profile");
        }
        if(request.getAccessType()== LeaveType.BIOMETRIC){
            if(employee.getBiometricStatus()== BiometricVpnStatus.PROVIDED){
                throw new BadRequestException("You already have biometric access");
            }
        }
        if(request.getAccessType()==LeaveType.VPN){
            if(employee.getVpnStatus()==BiometricVpnStatus.PROVIDED){
                throw new BadRequestException("You already have Vpn access");
            }
        }

        // Check if already has pending request of same type
        accessRequestRepository
                .findByEmployeeIdAndAccessType(employeeId, request.getAccessType())
                .ifPresent(existing -> {
                    if (existing.getStatus() == AccessRequestStatus.SUBMITTED ||
                            existing.getStatus() == AccessRequestStatus.MANAGER_APPROVED) {
                        throw new BadRequestException(
                                "You already have a pending " +
                                        request.getAccessType() + " access request");
                    }
                });

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setEmployeeId(employeeId);
        accessRequest.setAccessType(request.getAccessType());
        accessRequest.setReason(request.getReason());
        accessRequest.setReportingId(employee.getReportingId());
        accessRequest.setStatus(AccessRequestStatus.SUBMITTED);
        accessRequest.setSubmittedAt(LocalDateTime.now());

        AccessRequest saved = accessRequestRepository.save(accessRequest);

        // Notify manager
        notifyManager(employee, saved);

        return mapToResponseDto(saved);
    }

    /**
     * Employee views their access requests
     */
    public List<AccessRequestResponseDto> getMyAccessRequests(Long employeeId) {
        return accessRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }


    // ═══════════════════════════════════════════════════════════════════════
    // MANAGER ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all pending requests for a specific manager
     */
    public List<AccessRequestForManagerDto> getPendingRequestsForManager(Long managerId) {
        return accessRequestRepository.findPendingForManager(managerId)
                .stream()
                .map(this::mapToManagerDto)
                .collect(Collectors.toList());
    }

    /**
     * Manager approves/rejects access request
     */
    @Transactional
    public AccessRequestResponseDto managerDecision(
            Long requestId,
            ManagerAccessDecisionDto decision) {

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found"));

        // Verify manager owns this request
        if (!request.getReportingId().equals(decision.getReportingId())) {
            throw new BadRequestException("Unauthorized - this is not your request");
        }

        if (request.getStatus() != AccessRequestStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Request is not in SUBMITTED state. Current: " + request.getStatus());
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        boolean isApproved = "APPROVED".equalsIgnoreCase(decision.getDecision());

        if (isApproved) {
            request.setStatus(AccessRequestStatus.MANAGER_APPROVED);
            notifyEmployeeManagerApproved(employee, request);
            log.info("Manager {} approved {} request for employee {}",
                    decision.getReportingId(), request.getAccessType(), employee.getId());
        } else {
            if (decision.getRemarks() == null || decision.getRemarks().isBlank()) {
                throw new BadRequestException("Remarks are required when rejecting");
            }
            request.setStatus(AccessRequestStatus.MANAGER_REJECTED);
            request.setManagerRemarks(decision.getRemarks());
            notifyEmployeeManagerRejected(employee, request);
            log.info("Manager {} rejected {} request for employee {}",
                    decision.getReportingId(), request.getAccessType(), employee.getId());
        }

        request.setManagerDecision(isApproved ? "APPROVED" : "REJECTED");
        if (isApproved && decision.getRemarks() != null) {
            request.setManagerRemarks(decision.getRemarks());
        }
        request.setManagerDecisionAt(LocalDateTime.now());

        return mapToResponseDto(accessRequestRepository.save(request));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all manager-approved requests waiting for admin
     */
    public List<AccessRequestForAdminDto> getPendingAdminApprovals() {
        return accessRequestRepository.findManagerApprovedRequests()
                .stream()
                .map(this::mapToAdminDto)
                .collect(Collectors.toList());
    }

    /**
     * Admin approves/rejects (and grants actual access if approved)
     */
    @Transactional
    public AccessRequestResponseDto adminDecision(
            Long requestId,
            AdminAccessDecisionDto decision) {

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found"));

        if (request.getStatus() != AccessRequestStatus.MANAGER_APPROVED) {
            throw new BadRequestException(
                    "Request must be MANAGER_APPROVED before admin can decide. Current: " +
                            request.getStatus());
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        boolean isApproved = "APPROVED".equalsIgnoreCase(decision.getDecision());

        if (isApproved) {
            request.setStatus(AccessRequestStatus.ADMIN_APPROVED);

            // ✅ Grant actual access in Employee entity
            if (request.getAccessType() == LeaveType.VPN) {
                employee.setVpnStatus(BiometricVpnStatus.PROVIDED);
            } else if (request.getAccessType() == LeaveType.BIOMETRIC) {
                employee.setBiometricStatus(BiometricVpnStatus.PROVIDED);
            }
            employeeRepository.save(employee);

            notifyEmployeeAdminApproved(employee, request);
            log.info("Admin approved {} request for employee {}. Access granted.",
                    request.getAccessType(), employee.getId());
        } else {
            if (decision.getRemarks() == null || decision.getRemarks().isBlank()) {
                throw new BadRequestException("Remarks are required when rejecting");
            }
            request.setStatus(AccessRequestStatus.ADMIN_REJECTED);
            request.setAdminRemarks(decision.getRemarks());
            notifyEmployeeAdminRejected(employee, request);
            log.info("Admin rejected {} request for employee {}",
                    request.getAccessType(), employee.getId());
        }

        request.setAdminDecision(isApproved ? "APPROVED" : "REJECTED");
        if (!isApproved && decision.getRemarks() != null) {
            request.setAdminRemarks(decision.getRemarks());
        }
        if (decision.getRemarks() != null && isApproved) {
            request.setAdminRemarks(decision.getRemarks());
        }
        request.setAdminDecisionAt(LocalDateTime.now());

        return mapToResponseDto(accessRequestRepository.save(request));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS & MAPPING
    // ═══════════════════════════════════════════════════════════════════════

    private AccessRequestResponseDto mapToResponseDto(AccessRequest request) {
        AccessRequestResponseDto dto = new AccessRequestResponseDto();
        dto.setId(request.getId());
        dto.setEmployeeId(request.getEmployeeId());
        dto.setAccessType(request.getAccessType());
        dto.setStatus(request.getStatus());
        dto.setReason(request.getReason());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setManagerDecision(request.getManagerDecision());
        dto.setManagerRemarks(request.getManagerRemarks());
        dto.setManagerDecisionAt(request.getManagerDecisionAt());
        dto.setAdminDecision(request.getAdminDecision());
        dto.setAdminRemarks(request.getAdminRemarks());
        dto.setAdminDecisionAt(request.getAdminDecisionAt());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    private AccessRequestForManagerDto mapToManagerDto(AccessRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);

        AccessRequestForManagerDto dto = new AccessRequestForManagerDto();
        dto.setId(request.getId());
        dto.setEmployeeId(request.getEmployeeId());
        dto.setEmployeeName(employee != null ? employee.getName() : "Unknown");
        dto.setEmployeeEmail(employee != null ? employee.getEmail() : "Unknown");
        dto.setAccessType(request.getAccessType());
        dto.setStatus(request.getStatus());
        dto.setReason(request.getReason());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }

    private AccessRequestForAdminDto mapToAdminDto(AccessRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);
        Employee manager = employeeRepository.findById(request.getReportingId()).orElse(null);
        AccessRequestForAdminDto dto = new AccessRequestForAdminDto();
        dto.setId(request.getId());
        dto.setEmployeeId(request.getEmployeeId());
        dto.setEmployeeName(employee != null ? employee.getName() : "Unknown");
        dto.setEmployeeEmail(employee != null ? employee.getEmail() : "Unknown");

        // Get designation from personal details if available
        if (employee != null) {
            personalDetailsRepository.findByEmployeeId(employee.getId())
                    .ifPresent(pd -> dto.setEmployeeDesignation(pd.getDesignation()));
        }

        dto.setAccessType(request.getAccessType());
        dto.setStatus(request.getStatus());
        dto.setReason(request.getReason());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setManagerDecision(request.getManagerDecision());
        dto.setManagerRemarks(request.getManagerRemarks());
        dto.setManagerDecisionAt(request.getManagerDecisionAt());
        dto.setManagerName(manager != null ? manager.getName() : "Unknown");
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    // ─── NOTIFICATIONS ────────────────────────────────────────────────────

    private void notifyManager(Employee employee, AccessRequest request) {
        Employee manager = employeeRepository.findById(request.getReportingId()).orElse(null);
        if (manager == null) return;

        String message = "Employee " + employee.getName() + " has requested " +
                request.getAccessType() + " access. Reason: " +
                request.getReason() + ". Please review and approve/reject.";

        notificationService.createNotification(
                request.getReportingId(),
                "noreply@company.com",
                manager.getEmail(),
                EventType.ACCESS_REQUEST_SUBMITTED,
                manager.getRole(),
                Channel.EMAIL,
                message
        );
    }

    private void notifyEmployeeManagerApproved(Employee employee, AccessRequest request) {
        String message = "Your " + request.getAccessType() +
                " access request has been approved by your manager. It is now pending admin approval.";

        sendEmailNotification(employee, EventType.ACCESS_REQUEST_MANAGER_APPROVED, message);
    }

    private void notifyEmployeeManagerRejected(Employee employee, AccessRequest request) {
        String message = "Your " + request.getAccessType() +
                " access request has been rejected by your manager. Reason: " +
                request.getManagerRemarks();

        sendEmailNotification(employee, EventType.ACCESS_REQUEST_MANAGER_REJECTED, message);
    }

    private void notifyEmployeeAdminApproved(Employee employee, AccessRequest request) {
        String message = "Your " + request.getAccessType() +
                " access request has been approved by admin. Access will be granted shortly.";

        sendEmailNotification(employee, EventType.ACCESS_REQUEST_ADMIN_APPROVED, message);
    }

    private void notifyEmployeeAdminRejected(Employee employee, AccessRequest request) {
        String message = "Your " + request.getAccessType() +
                " access request has been rejected by admin. Reason: " +
                request.getAdminRemarks();

        sendEmailNotification(employee, EventType.ACCESS_REQUEST_ADMIN_REJECTED, message);
    }

    private void sendEmailNotification(Employee employee, EventType eventType, String message) {
        notificationService.createNotification(
                employee.getId(),
                "noreply@company.com",
                employee.getEmail(),
                eventType,
                employee.getRole(),
                Channel.EMAIL,
                message
        );
    }
}