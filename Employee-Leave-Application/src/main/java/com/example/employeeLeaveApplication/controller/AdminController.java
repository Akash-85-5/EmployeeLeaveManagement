package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import com.example.employeeLeaveApplication.enums.EmployeeType;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.service.AccessRequestService;
import com.example.employeeLeaveApplication.service.AdminService;
import com.example.employeeLeaveApplication.service.CarryForwardService;
import com.example.employeeLeaveApplication.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final CarryForwardService carryForwardService;
    private final EmployeeService employeeService;
    private final AccessRequestService accessRequestService;

    public AdminController(CarryForwardService carryForwardService,
                           AdminService adminService,
                           EmployeeService employeeService,
                           AccessRequestService accessRequestService) {
        this.carryForwardService = carryForwardService;
        this.adminService = adminService;
        this.employeeService = employeeService;
        this.accessRequestService = accessRequestService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING ENDPOINTS (UNCHANGED)
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/carry-forward")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processCarryForward(@RequestParam Integer fromYear) {
        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok("Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId, @RequestParam Integer fromYear) {
        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok("Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/users/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        adminService.createUser(request);
        return ResponseEntity.ok("User created successfully");
    }

    @PostMapping("/reset-password/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetPassword(@PathVariable Long userId) {
        adminService.resetPassword(userId);
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/eligible-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDropdownResponse> getEligibleManagers(@RequestParam Role role) {
        return adminService.getEligibleManagers(role);
    }

    @PutMapping("/employees/{employeeId}/pf-number")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> updatePfNumber(
            @PathVariable Long employeeId,
            @RequestBody PfUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updatePfNumber(employeeId, request));
    }

    @PutMapping(value = "/employees/{employeeId}/personal-details",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> adminUpdatePersonalDetails(
            @PathVariable Long employeeId,
            @RequestParam EmployeeType employeeType,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "aadhaarCard", required = false) MultipartFile aadhaarCard,
            @RequestPart(value = "doc1", required = false) MultipartFile doc1,
            @RequestPart(value = "doc2", required = false) MultipartFile doc2) {

        return ResponseEntity.ok(
                employeeService.adminUpdatePersonalDetails(
                        employeeId, dataJson, aadhaarCard, doc1, doc2, employeeType));
    }

    @GetMapping("/employees/{employeeId}/personal-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }

    @GetMapping("/onboarding/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employee>> getOnboardingPending(){
        return ResponseEntity.ok(employeeService.getOnboardingPending());
    }

    @PatchMapping("/onboarding/bio/decision/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bioDecide(@PathVariable Long employeeId,
                                            @RequestParam BiometricVpnStatus decision){
        System.out.println(decision);
        employeeService.decideBio(employeeId, decision);
        return ResponseEntity.ok("Decision recorded: " + decision);
    }

    @PatchMapping("/onboarding/vpn/decision/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> vpnDecide(@PathVariable Long employeeId,
                                            @RequestParam BiometricVpnStatus decision){
        employeeService.decideVpn(employeeId, decision);
        return ResponseEntity.ok("Decision recorded: " + decision);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW: ACCESS REQUEST MANAGEMENT ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all pending access requests (manager-approved, waiting for admin)
     * GET /api/admin/access-requests/pending
     *
     * Shows:
     * - Employee details (name, email, designation)
     * - Access type (VPN/BIOMETRIC)
     * - Original reason
     * - Manager's decision and remarks
     * - Manager name
     */
    @GetMapping("/access-requests/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccessRequestForAdminDto>> getPendingAccessRequests() {
        log.info("Admin viewing pending access request approvals");
        List<AccessRequestForAdminDto> requests =
                accessRequestService.getPendingAdminApprovals();
        return ResponseEntity.ok(requests);
    }

    /**
     * Admin approves or rejects access request
     * PATCH /api/admin/access-requests/{requestId}/decision
     *
     * Request body:
     * {
     *   "decision": "APPROVED",  // or "REJECTED"
     *   "remarks": "Access provisioned"  (optional)
     * }
     *
     * If APPROVED:
     * - Request status = ADMIN_APPROVED
     * - Employee.vpnStatus or Employee.biometricStatus = PROVIDED
     * - Access is IMMEDIATELY GRANTED
     *
     * If REJECTED:
     * - Request status = ADMIN_REJECTED
     * - adminRemarks stored
     * - No access granted
     */
    @PatchMapping("/access-requests/{requestId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessRequestResponseDto> approveAccessRequest(
            @PathVariable Long requestId,
            @RequestBody AdminAccessDecisionDto decision) {

        log.info("Admin making decision on access request {}: {}",
                requestId, decision.getDecision());

        AccessRequestResponseDto response =
                accessRequestService.adminDecision(requestId, decision);

        return ResponseEntity.ok(response);
    }

    /**
     * Get access request details for admin review
     * GET /api/admin/access-requests/{requestId}
     */
    @GetMapping("/access-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessRequestForAdminDto> getAccessRequestDetails(
            @PathVariable Long requestId) {

        log.info("Admin viewing access request details: {}", requestId);

        // Note: This requires adding a new method to service
        // For now, use the list and find - better to add dedicated method
        List<AccessRequestForAdminDto> all =
                accessRequestService.getPendingAdminApprovals();

        AccessRequestForAdminDto request = all.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Request not found or not pending"));

        return ResponseEntity.ok(request);
    }

    /**
     * Get all access requests (all statuses, for audit trail)
     * GET /api/admin/access-requests/all
     *
     * Returns requests in all statuses for historical view
     */
    @GetMapping("/access-requests/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccessRequestResponseDto>> getAllAccessRequests() {
        log.info("Admin viewing all access requests");
        // Note: This requires adding a new method to service - getAllAccessRequests()
        // Currently the service has methods for specific statuses
        // You may want to add: service.getAllAccessRequests()
        return ResponseEntity.ok(List.of());
    }
}