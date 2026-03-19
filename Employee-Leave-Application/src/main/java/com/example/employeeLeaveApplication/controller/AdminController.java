package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.*;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.enums.BiometricVpnStatus;
import com.example.employeeLeaveApplication.enums.EmployeeType;
import com.example.employeeLeaveApplication.enums.Role;
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

    public AdminController(CarryForwardService carryForwardService,
                           AdminService adminService,
                           EmployeeService employeeService) {
        this.carryForwardService = carryForwardService;
        this.adminService = adminService;
        this.employeeService = employeeService;
    }

    // ── UNCHANGED ─────────────────────────────────────────────────
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

    // ── NEW: Admin sets PF number after employee profile is verified
    /**
     * PUT /api/admin/employees/{employeeId}/pf-number
     * Body: { "pfNumber": "PF001234" }
     * Employee does NOT fill this — admin fills after verification.
     */
    @PutMapping("/employees/{employeeId}/pf-number")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> updatePfNumber(
            @PathVariable Long employeeId,
            @RequestBody PfUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updatePfNumber(employeeId, request));
    }

    // ── NEW: Admin override for personal details (multipart) ──────
    /**
     * PUT /api/admin/employees/{employeeId}/personal-details?employeeType=FRESHER
     *
     * Admin can update any employee's personal details bypassing lock.
     * Sends same multipart format as employee submit.
     * Sets verificationStatus = VERIFIED directly.
     *
     * Parts:
     *   data        → JSON string
     *   aadhaarCard → file (optional if not replacing)
     *   doc1        → tc OR experienceCertificate (optional)
     *   doc2        → offerLetter OR leavingLetter (optional)
     */
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

    // ── UNCHANGED: view personal details ─────────────────────────
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

    @PatchMapping("/onBoarding/bio/decision/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> bioDecide(@PathVariable Long employeeId,
                                                         @RequestParam BiometricVpnStatus decision){
        employeeService.decideBio(employeeId, decision);
        return ResponseEntity.ok("Decision recorder " + decision);
    }
    @PatchMapping("/onBoarding/vpn/decision/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> vpnDecide(@PathVariable Long employeeId,
                                                       @RequestParam BiometricVpnStatus decision){
        employeeService.decideVpn(employeeId, decision);
        return ResponseEntity.ok("Decision recorder " + decision);
    }
}