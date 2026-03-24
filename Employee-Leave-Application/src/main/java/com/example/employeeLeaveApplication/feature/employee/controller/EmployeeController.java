package com.example.employeeLeaveApplication.feature.employee.controller;

import com.example.employeeLeaveApplication.feature.employee.dto.ProfileResponse;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── UNCHANGED ─────────────────────────────────────────────────
    @GetMapping("/profile/{employeeId}")
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getProfile(employeeId));
    }

    /**
     * FRESHER personal details + documents in one multipart request.
     *
     * Content-Type: multipart/form-data
     *
     * Parts:
     *   data         → JSON string of FresherPersonalDetailsRequest
     *   aadhaarCard  → file (PDF/JPG/PNG, max 5MB)
     *   tc           → file (Transfer Certificate)
     *   offerLetter  → file (Offer Letter)
     *
     * Example (Postman / Axios):
     *   const form = new FormData();
     *   form.append('data', JSON.stringify({ fullName: '...', ... }));
     *   form.append('aadhaarCard', aadhaarFile);
     *   form.append('tc', tcFile);
     *   form.append('offerLetter', offerLetterFile);
     *   axios.post('/api/employees/personal-details/42/fresher', form);
     */
    @PostMapping(value = "/personal-details/{employeeId}/fresher",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<EmployeePersonalDetails> submitFresherDetails(
            @PathVariable Long employeeId,
            @RequestPart("data") String dataJson,
            @RequestPart("aadhaarCard") MultipartFile aadhaarCard,
            @RequestPart("tc") MultipartFile tc,
            @RequestPart("offerLetter") MultipartFile offerLetter) {

        return ResponseEntity.ok(
                employeeService.submitFresherDetails(
                        employeeId, dataJson, aadhaarCard, tc, offerLetter));
    }

    /**
     * EXPERIENCED personal details + documents in one multipart request.
     *
     * Content-Type: multipart/form-data
     *
     * Parts:
     *   data                  → JSON string of ExperiencedPersonalDetailsRequest
     *   aadhaarCard           → file
     *   experienceCertificate → file
     *   leavingLetter         → file
     */
    @PostMapping(value = "/personal-details/{employeeId}/experienced",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("#employeeId == authentication.principal.user.id")
    public ResponseEntity<EmployeePersonalDetails> submitExperiencedDetails(
            @PathVariable Long employeeId,
            @RequestPart("data") String dataJson,
            @RequestPart("aadhaarCard") MultipartFile aadhaarCard,
            @RequestPart("experienceCertificate") MultipartFile experienceCertificate,
            @RequestPart("leavingLetter") MultipartFile leavingLetter) {

        return ResponseEntity.ok(
                employeeService.submitExperiencedDetails(
                        employeeId, dataJson, aadhaarCard, experienceCertificate, leavingLetter));
    }

    // ── UNCHANGED ─────────────────────────────────────────────────
    @GetMapping("/personal-details/{employeeId}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR','CFO','ADMIN')")
    public Page<Employee> getAllEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return employeeService.getAllEmployees(name, email, role, managerId, active, pageable);
    }

    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasRole('MANAGER') and #managerId == authentication.principal.user.id")
    public List<Employee> getTeamMembers(@PathVariable Long managerId) {
        return employeeService.getTeamMembers(managerId);
    }

//    @GetMapping("/teamleader/{teamLeaderId}/team")
//    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id")
//    public List<Employee> getTeamLeaderMembers(@PathVariable Long teamLeaderId) {
//        return employeeService.getTeamLeaderMembers(teamLeaderId);
//    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('CFO')")
    public List<Employee> searchEmployees(@RequestParam String query) {
        return employeeService.searchEmployees(query);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }
}