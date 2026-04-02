package com.emp_management.feature.employee.controller;

import com.emp_management.feature.employee.dto.EmployeeResponseDTO;
import com.emp_management.feature.employee.dto.NameDto;
import com.emp_management.feature.employee.dto.ProfileResponse;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
import com.emp_management.feature.employee.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── UNCHANGED ─────────────────────────────────────────────────
    @GetMapping("/profile/{employeeId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getProfile(employeeId));
    }

    @GetMapping("/name/{emp_id}")
    public ResponseEntity<NameDto> getEmpName(@PathVariable String emp_id){
        return ResponseEntity.ok(employeeService.getEmployeeName(emp_id));
    }
    @PostMapping(value = "/personal-details/{employeeId}/fresher",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> submitFresherDetails(
            @PathVariable String employeeId,
            @RequestPart("data") String dataJson,
            @RequestPart("aadhaarCard") MultipartFile aadhaarCard,
            @RequestPart("tc") MultipartFile tc,
            @RequestPart("offerLetter") MultipartFile offerLetter) {

        return ResponseEntity.ok(
                employeeService.submitFresherDetails(
                        employeeId, dataJson, aadhaarCard, tc, offerLetter));
    }

    @PostMapping(value = "/personal-details/{employeeId}/experienced",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> submitExperiencedDetails(
            @PathVariable String employeeId,
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
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
    }

    @GetMapping("/all")
    public Page<EmployeeResponseDTO> getAllEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String managerId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return employeeService.getAllEmployees(
                name, email, role, managerId, active, pageable);
    }

    @GetMapping("/manager/{managerId}/team")
    public List<Employee> getTeamMembers(@PathVariable String managerId) {
        return employeeService.getTeamMembers(managerId);
    }

//    @GetMapping("/teamleader/{teamLeaderId}/team")
//    @PreAuthorize("hasRole('TEAM_LEADER') and #teamLeaderId == authentication.principal.user.id")
//    public List<Employee> getTeamLeaderMembers(@PathVariable Long teamLeaderId) {
//        return employeeService.getTeamLeaderMembers(teamLeaderId);
//    }

    @GetMapping("/search")
    public List<Employee> searchEmployees(@RequestParam String query) {
        return employeeService.searchEmployees(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }
//    @GetMapping("/me")
//    public EmployeeProfileResponse getMyProfile(Authentication authentication) {
//
//        CustomUserDetails userDetails =
//                (CustomUserDetails) authentication.getPrincipal();
//
//        User user = userDetails.getUser();
//
//        return employeeService.getMyProfile(user);
//    }
}