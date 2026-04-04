package com.emp_management.feature.employee.controller;

import com.emp_management.feature.employee.dto.EmployeeResponseDTO;
import com.emp_management.feature.employee.dto.NameDto;
import com.emp_management.feature.employee.dto.ProfileResponse;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
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

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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
    // FRESHER
    // ─────────────────────────────────────────────────────────────

    /**
     * POST — first-time submission by a fresher.
     * Fails if a record already exists in PENDING or VERIFIED state.
     *
     * Multipart keys:
     *   data              – JSON (FresherPersonalDetailsRequest)
     *   idProof           – ID proof document
     *   tenthMarksheet    – 10th marksheet
     *   twelfthMarksheet  – 12th marksheet
     *   degreeCertificate – Degree / Provisional certificate
     *   offerLetter       – Offer letter
     *   passportPhoto     – Passport-size photo
     */
    @PostMapping(value = "/personal-details/{employeeId}/fresher",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> submitFresherDetails(
            @PathVariable String employeeId,
            @RequestPart("data")               String dataJson,
            @RequestPart("idProof")            MultipartFile idProof,
            @RequestPart("tenthMarksheet")     MultipartFile tenthMarksheet,
            @RequestPart("twelfthMarksheet")   MultipartFile twelfthMarksheet,
            @RequestPart("degreeCertificate")  MultipartFile degreeCertificate,
            @RequestPart("offerLetter")        MultipartFile offerLetter,
            @RequestPart("passportPhoto")      MultipartFile passportPhoto) {

        return ResponseEntity.ok(
                employeeService.submitFresherDetails(
                        employeeId, dataJson,
                        idProof, tenthMarksheet, twelfthMarksheet,
                        degreeCertificate, offerLetter, passportPhoto));
    }

    /**
     * PUT — resubmission after HR rejection.
     * Fails unless the current verification status is REJECTED.
     * All 6 documents must be re-uploaded (full replacement — no partial update).
     *
     * Multipart keys: same as POST above.
     */
    @PutMapping(value = "/personal-details/{employeeId}/fresher",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> updateFresherDetails(
            @PathVariable String employeeId,
            @RequestPart("data")               String dataJson,
            @RequestPart("idProof")            MultipartFile idProof,
            @RequestPart("tenthMarksheet")     MultipartFile tenthMarksheet,
            @RequestPart("twelfthMarksheet")   MultipartFile twelfthMarksheet,
            @RequestPart("degreeCertificate")  MultipartFile degreeCertificate,
            @RequestPart("offerLetter")        MultipartFile offerLetter,
            @RequestPart("passportPhoto")      MultipartFile passportPhoto) {

        return ResponseEntity.ok(
                employeeService.updateFresherDetails(
                        employeeId, dataJson,
                        idProof, tenthMarksheet, twelfthMarksheet,
                        degreeCertificate, offerLetter, passportPhoto));
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCED
    // ─────────────────────────────────────────────────────────────

    /**
     * POST — first-time submission by an experienced employee.
     * Fails if a record already exists in PENDING or VERIFIED state.
     *
     * Multipart keys:
     *   data              – JSON (ExperiencedPersonalDetailsRequest)
     *                       The "experiences" array index must match
     *                       the order of files in "experienceCerts".
     *   idProof           – ID proof document (single file)
     *   passportPhoto     – Passport-size photo (single file)
     *   experienceCerts   – One file per experience entry (ordered list)
     *   relievingLetter   – Relieving letter from the last company (single file)
     */
    @PostMapping(value = "/personal-details/{employeeId}/experienced",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> submitExperiencedDetails(
            @PathVariable String employeeId,
            @RequestPart("data")             String dataJson,
            @RequestPart("idProof")          MultipartFile idProof,
            @RequestPart("passportPhoto")    MultipartFile passportPhoto,
            @RequestPart("experienceCerts")  List<MultipartFile> experienceCerts,
            @RequestPart("relievingLetter")  MultipartFile relievingLetter) {

        return ResponseEntity.ok(
                employeeService.submitExperiencedDetails(
                        employeeId, dataJson,
                        idProof, passportPhoto, experienceCerts, relievingLetter));
    }

    /**
     * PUT — resubmission after HR rejection.
     * Fails unless the current verification status is REJECTED.
     * All documents must be re-uploaded (full replacement — no partial update).
     *
     * Multipart keys: same as POST above.
     */
    @PutMapping(value = "/personal-details/{employeeId}/experienced",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeePersonalDetails> updateExperiencedDetails(
            @PathVariable String employeeId,
            @RequestPart("data")             String dataJson,
            @RequestPart("idProof")          MultipartFile idProof,
            @RequestPart("passportPhoto")    MultipartFile passportPhoto,
            @RequestPart("experienceCerts")  List<MultipartFile> experienceCerts,
            @RequestPart("relievingLetter")  MultipartFile relievingLetter) {

        return ResponseEntity.ok(
                employeeService.updateExperiencedDetails(
                        employeeId, dataJson,
                        idProof, passportPhoto, experienceCerts, relievingLetter));
    }

    // ── Personal details read ─────────────────────────────────────
    @GetMapping("/personal-details/{employeeId}")
    public ResponseEntity<EmployeePersonalDetails> getPersonalDetails(
            @PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getPersonalDetails(employeeId));
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
    public List<Employee> searchEmployees(@RequestParam String query) {
        return employeeService.searchEmployees(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }
}