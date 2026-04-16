package com.emp_management.feature.employee.controller;

import com.emp_management.feature.employee.dto.EmployeeResponseDTO;
import com.emp_management.feature.employee.dto.NameDto;
import com.emp_management.feature.employee.dto.ProfileResponse;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.employee.service.EmployeeService;
import com.emp_management.shared.dto.BranchListDto;
import com.emp_management.shared.dto.EmployeeListDto;
import com.emp_management.shared.entity.Department;
import com.emp_management.shared.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeService employeeService,
                              EmployeeRepository employeeRepository) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
    }

    // ── Lookups ───────────────────────────────────────────────────

    @GetMapping("/profile/{employeeId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getProfile(employeeId));
    }

    @GetMapping("/name/{emp_id}")
    public ResponseEntity<NameDto> getEmpName(@PathVariable String emp_id) {
        return ResponseEntity.ok(employeeService.getEmployeeName(emp_id));
    }

    @GetMapping("/departments/list")
    public ResponseEntity<List<Department>> getDepartmentList() {
        return ResponseEntity.ok(employeeService.getDepartmentList());
    }

    @GetMapping("/role/list")
    public ResponseEntity<List<Role>> getRoleList() {
        return ResponseEntity.ok(employeeService.getRoleList());
    }

    @GetMapping("/managers/list")
    public ResponseEntity<List<EmployeeListDto>> getAllEmployeesList() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/branch/list")
    public ResponseEntity<List<BranchListDto>> getAllBranches() {
        return ResponseEntity.ok(employeeService.getAllBranches());
    }

    // ─────────────────────────────────────────────────────────────
    // FRESHER — POST (first-time submission, all fields + files mandatory)
    // ─────────────────────────────────────────────────────────────

    /**
     * Multipart keys:
     *   data              – JSON (FresherPersonalDetailsRequest)
     *   idProof, tenthMarksheet, twelfthMarksheet,
     *   degreeCertificate, offerLetter, passportPhoto
     */
    @PostMapping(value = "/personal-details/{employeeId}/fresher",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitFresherDetails(
            @PathVariable String employeeId,
            @RequestPart("data")               String dataJson,
            @RequestPart("idProof")            MultipartFile idProof,
            @RequestPart("tenthMarksheet")     MultipartFile tenthMarksheet,
            @RequestPart("twelfthMarksheet")   MultipartFile twelfthMarksheet,
            @RequestPart("degreeCertificate")  MultipartFile degreeCertificate,
            @RequestPart("offerLetter")        MultipartFile offerLetter,
            @RequestPart("passportPhoto")      MultipartFile passportPhoto) {

        employeeService.submitFresherDetails(employeeId, dataJson,
                idProof, tenthMarksheet, twelfthMarksheet,
                degreeCertificate, offerLetter, passportPhoto);
        return ResponseEntity.ok("Personal details submitted successfully.");
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCED — POST (first-time submission, all fields + files mandatory)
    // ─────────────────────────────────────────────────────────────

    /**
     * Multipart keys:
     *   data              – JSON (ExperiencedPersonalDetailsRequest)
     *                       "experiences" array index must match file lists.
     *   idProof           – single file
     *   passportPhoto     – single file
     *   experienceCerts   – one file per experience entry (indexed list)
     *   joiningLetters    – optional indexed list; send null/absent for entries without one
     *   relievingLetter   – single file for the last-company entry
     */
    @PostMapping(value = "/personal-details/{employeeId}/experienced",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitExperiencedDetails(
            @PathVariable String employeeId,
            @RequestPart("data")                                       String dataJson,
            @RequestPart("idProof")                                    MultipartFile idProof,
            @RequestPart("passportPhoto")                              MultipartFile passportPhoto,
            @RequestPart("experienceCerts")                            List<MultipartFile> experienceCerts,
            @RequestPart(value = "joiningLetters", required = false)   List<MultipartFile> joiningLetters,
            @RequestPart("relievingLetters")                            List<MultipartFile> relievingLetters) {
        employeeService.submitExperiencedDetails(employeeId, dataJson,
                idProof, passportPhoto, experienceCerts, joiningLetters, relievingLetters);
        return ResponseEntity.ok("Personal details submitted successfully.");
    }

    @PutMapping(value = "/profile/{employeeId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfile(
            @PathVariable String employeeId,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "idProof",           required = false)    MultipartFile idProof,
            @RequestPart(value = "passportPhoto",     required = false)    MultipartFile passportPhoto,
            @RequestPart(value = "tenthMarksheet",    required = false)    MultipartFile tenthMarksheet,
            @RequestPart(value = "twelfthMarksheet",  required = false)    MultipartFile twelfthMarksheet,
            @RequestPart(value = "degreeCertificate", required = false)    MultipartFile degreeCertificate,
            @RequestPart(value = "offerLetter",       required = false)    MultipartFile offerLetter,
            @RequestPart(value = "experienceCerts",   required = false)    List<MultipartFile> experienceCerts,
            @RequestPart(value = "joiningLetters",    required = false)    List<MultipartFile> joiningLetters,
            @RequestPart(value = "relievingLetters",   required = false)    List<MultipartFile> relievingLetters) {

        employeeService.updateProfile(employeeId, dataJson,
                idProof, passportPhoto,
                tenthMarksheet, twelfthMarksheet, degreeCertificate, offerLetter,
                experienceCerts, joiningLetters, relievingLetters);

        return ResponseEntity.ok("Profile updated successfully.");
    }

    // ── Personal details read ─────────────────────────────────────

    @GetMapping("/personal-details/{employeeId}")
    public ResponseEntity<ProfileResponse> getPersonalDetails(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetailsAsProfile(employeeId));
    }

    // ── Employee list / search ────────────────────────────────────

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
        return employeeService.getAllEmployees(name, email, role, managerId, active, pageable);
    }

    @GetMapping("/manager/{managerId}/team")
    public List<Employee> getTeamMembers(@PathVariable String managerId) {
        return employeeService.getTeamMembers(managerId);
    }

    @GetMapping("/search")
    public List<EmployeeResponseDTO> searchEmployees(@RequestParam String query) {
        return employeeService.globalSearch(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully.");
    }
    @GetMapping("/by-email")
    public ResponseEntity<?> getEmployeeByEmail(
            @RequestParam String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found: " + email));

        Map<String, String> result = new HashMap<>();
        result.put("empId", employee.getEmpId());
        result.put("name", employee.getName());
        result.put("email", employee.getEmail());

        return ResponseEntity.ok(result);
    }
}