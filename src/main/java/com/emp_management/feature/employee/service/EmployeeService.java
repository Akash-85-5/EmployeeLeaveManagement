package com.emp_management.feature.employee.service;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.employee.dto.*;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import com.emp_management.feature.employee.entity.EmployeePersonalDetails;
import com.emp_management.feature.employee.repository.EmployeeOnboardingRepository;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.infrastructure.storage.DocumentStorageService;
import com.emp_management.shared.enums.*;
import com.emp_management.shared.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    // HR is hardcoded as id=2 since there is only one HR
    private static final Long HR_ID = 2L;

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final NotificationService notificationService;
    private final LeaveAllocationService leaveAllocationService;
    private final DocumentStorageService documentStorageService;
    private final EmployeeOnboardingRepository employeeOnboardingRepository;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,LeaveAllocationService leaveAllocationService, EmployeePersonalDetailsRepository personalDetailsRepository, NotificationService notificationService, DocumentStorageService documentStorageService, EmployeeOnboardingRepository employeeOnboardingRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
        this.notificationService = notificationService;
        this.documentStorageService = documentStorageService;
        this.employeeOnboardingRepository = employeeOnboardingRepository;
        this.leaveAllocationService = leaveAllocationService;

    }

    public NameDto getEmployeeName(String empId){
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(()-> new EntityNotFoundException("Employee not found"));
        NameDto name = new NameDto();
        name.setEmpId(employee.getEmpId());
        name.setEmpName(employee.getName());
        return name;
    }
    // ─── getProfile ───────────────────────────────────────────────

    public ProfileResponse getProfile(String  employeeId) {

        User user = userRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));


        ProfileResponse response = new ProfileResponse();

        // ── Unchanged block ───────────────────────────────────────
        response.setId(employee.getEmpId());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setRole(employee.getRole().getRoleName());
        response.setReportingId(employee.getReportingId());
        response.setEmployeeExperience(employee.getEmployeeExperience());
        response.setActive(user.getStatus() == EmployeeStatus.ACTIVE);
        response.setMustChangePassword(user.isForcePwdChange());
        response.setJoiningDate(employee.getOnboarding().getJoiningDate());
        response.setBiometricStatus(employee.getOnboarding().getBiometricStatus().name());
        response.setVpnStatus(employee.getOnboarding().getVpnStatus().name());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        response.setBranch(employee.getBranch().getName());
        response.setCompanyName(employee.getBranch().getCompany().getName());
        response.setCountry(employee.getBranch().getCompany().getCountry().getName());

        if (employee.getReportingId() != null) {
            employeeRepository.findByEmpId(employee.getReportingId())
                    .ifPresent(m -> response.setReportingName(m.getName()));
        }


        // ── Personal details ──────────────────────────────────────
        Optional<EmployeePersonalDetails> personalOpt =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        if (personalOpt.isPresent()) {
            EmployeePersonalDetails pd = personalOpt.get();
            mapPersonalDetailsToResponse(pd, response);
            response.setPersonalDetailsComplete(true);
            response.setPersonalDetailsLocked(pd.isLocked());
            response.setVerificationStatus(pd.getVerificationStatus());
            // Show HR remarks only when REJECTED
            if (pd.getVerificationStatus() == VerificationStatus.REJECTED) {
                response.setHrRemarks(pd.getHrRemarks());
            }
        } else {
            response.setPersonalDetailsComplete(false);
            response.setPersonalDetailsLocked(false);
            response.setVerificationStatus(null);
        }

        return response;
    }

    // ─── FRESHER: submit personal details + documents together ────

    /**
     * Single multipart request contains:
     * - data         : JSON string of FresherPersonalDetailsRequest
     * - aadhaarCard  : MultipartFile
     * - tc           : MultipartFile
     * - offerLetter  : MultipartFile
     * <p>
     * Employee can resubmit only if status = REJECTED.
     * After submit → status = PENDING → HR (id=2) notified.
     */
    @Transactional
    public EmployeePersonalDetails submitFresherDetails(
            String employeeId,
            String dataJson,
            MultipartFile aadhaarCard,
            MultipartFile tc,
            MultipartFile offerLetter) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        // Parse JSON part into DTO
        FresherPersonalDetailsRequest request = parseJson(dataJson, FresherPersonalDetailsRequest.class);

        // Validate all three documents are present
        validateFile(aadhaarCard, "Aadhaar card");
        validateFile(tc, "Transfer Certificate (TC)");
        validateFile(offerLetter, "Offer Letter");

        // Check existing submission state
        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        if (existing.isPresent()) {
            VerificationStatus status = existing.get().getVerificationStatus();
            if (status == VerificationStatus.PENDING)
                throw new BadRequestException(
                        "Your profile is already submitted and pending HR verification.");
            if (status == VerificationStatus.VERIFIED)
                throw new BadRequestException(
                        "Your profile is already verified. Contact Admin/HR for updates.");
            // REJECTED → delete old files so disk stays clean
            deleteOldFresherDocs(existing.get());
        }

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());

        // Fill all text fields
        fillCommonFields(pd, request);
        // UNA not needed for fresher
        pd.setUnaNumber(null);
        // Clear experienced-only fields
        clearExperiencedFields(pd);

        // Save documents to disk, store paths in entity
        pd.setAadhaarDocPath(documentStorageService.save(aadhaarCard, "aadhaar", employeeId));
        pd.setTcDocPath(documentStorageService.save(tc, "tc", employeeId));
        pd.setOfferLetterDocPath(documentStorageService.save(offerLetter, "offer-letter", employeeId));

        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.PENDING);
        pd.setHrRemarks(null);
        pd.setSubmittedAt(LocalDateTime.now());

        EmployeePersonalDetails saved = personalDetailsRepository.save(pd);
        leaveAllocationService.allocateForNewEmployee(employeeId);

//        Long roleId = request.getRoleId();
//        if (roleId != 1 && roleId != 2) {
//            try {
//                leaveAllocationService.allocateForNewEmployee(savedEmp.getEmpId());
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to create leave allocations: " + e.getMessage());
//            }

        // Notify HR (hardcoded id=2)
        notifyHr(employee.getName(), employeeId);

        return saved;
    }

    // ─── EXPERIENCED: submit personal details + documents together ─

    /**
     * Single multipart request contains:
     * - data                  : JSON string of ExperiencedPersonalDetailsRequest
     * - aadhaarCard           : MultipartFile
     * - experienceCertificate : MultipartFile
     * - leavingLetter         : MultipartFile
     */
    @Transactional
    public EmployeePersonalDetails submitExperiencedDetails(
            String  employeeId,
            String dataJson,
            MultipartFile aadhaarCard,
            MultipartFile experienceCertificate,
            MultipartFile leavingLetter) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        ExperiencedPersonalDetailsRequest request =
                parseJson(dataJson, ExperiencedPersonalDetailsRequest.class);

        validateFile(aadhaarCard, "Aadhaar card");
        validateFile(experienceCertificate, "Experience Certificate");
        validateFile(leavingLetter, "Leaving Letter");

        if (request.getUnaNumber() == null || request.getUnaNumber().isBlank())
            throw new BadRequestException("UNA number is required for experienced employees.");

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        if (existing.isPresent()) {
            VerificationStatus status = existing.get().getVerificationStatus();
            if (status == VerificationStatus.PENDING)
                throw new BadRequestException(
                        "Your profile is already submitted and pending HR verification.");
            if (status == VerificationStatus.VERIFIED)
                throw new BadRequestException(
                        "Your profile is already verified. Contact Admin/HR for updates.");
            deleteOldExperiencedDocs(existing.get());
        }

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());

        fillCommonFields(pd, request);
        pd.setUnaNumber(request.getUnaNumber());
        pd.setPreviousRole(request.getPreviousRole());
        pd.setOldCompanyName(request.getOldCompanyName());
        pd.setOldCompanyFromDate(request.getOldCompanyFromDate());
        pd.setOldCompanyEndDate(request.getOldCompanyEndDate());
        // Clear fresher-only fields
        clearFresherFields(pd);

        pd.setAadhaarDocPath(documentStorageService.save(aadhaarCard, "aadhaar", employeeId));
        pd.setExperienceCertDocPath(documentStorageService.save(experienceCertificate, "experience-cert", employeeId));
        pd.setLeavingLetterDocPath(documentStorageService.save(leavingLetter, "leaving-letter", employeeId));

        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.PENDING);
        pd.setHrRemarks(null);
        pd.setSubmittedAt(LocalDateTime.now());

        EmployeePersonalDetails saved = personalDetailsRepository.save(pd);

        notifyHr(employee.getName(), employeeId);

        return saved;
    }

    // ─── HR: get pending verifications ───────────────────────────

    public List<EmployeePersonalDetails> getPendingVerifications() {
        return personalDetailsRepository.findByVerificationStatus(VerificationStatus.PENDING);
    }

    public List<EmployeePersonalDetails> getAllVerifications() {
        return personalDetailsRepository.findAllByOrderBySubmittedAtDesc();
    }

    // ─── HR: verify or reject ─────────────────────────────────────

    @Transactional
    public EmployeePersonalDetails verifyPersonalDetails(
            String employeeId, HrVerificationRequest request) {

        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "No personal details found for employee: " + employeeId));

        if (pd.getVerificationStatus() != VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Profile is not in PENDING state. Current: " + pd.getVerificationStatus());

        if (request.getStatus() == VerificationStatus.PENDING)
            throw new BadRequestException("Cannot set status back to PENDING.");

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        pd.setVerificationStatus(request.getStatus());
        pd.setVerifiedAt(LocalDateTime.now());

        if (request.getStatus() == VerificationStatus.REJECTED) {
            if (request.getRemarks() == null || request.getRemarks().isBlank())
                throw new BadRequestException("Remarks are required when rejecting.");
            pd.setHrRemarks(request.getRemarks());
            pd.setLocked(false); // unlock so employee can resubmit
            notifyEmployee(employee, EventType.PROFILE_REJECTED,
                    "Hi " + employee.getName() + ", your profile was rejected. Reason: "
                            + request.getRemarks() + ". Please resubmit.");
        } else {
            pd.setHrRemarks(null);
            pd.setLocked(true);
            notifyEmployee(employee, EventType.PROFILE_VERIFIED,
                    "Hi " + employee.getName() + ", your profile has been verified by HR.");
        }

        return personalDetailsRepository.save(pd);
    }

    // ─── Admin: set PF number after verification ──────────────────

    @Transactional
    public EmployeePersonalDetails updatePfNumber(String employeeId, PfUpdateRequest request) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not found for employee: " + employeeId));
        pd.setPfNumber(request.getPfNumber());
        return personalDetailsRepository.save(pd);
    }

    // ─── Admin/HR override (bypasses lock, sets VERIFIED) ─────────

    @Transactional
    public EmployeePersonalDetails adminUpdatePersonalDetails(
            String employeeId,
            String dataJson,
            MultipartFile aadhaarCard,
            MultipartFile doc1,      // tc OR experienceCertificate
            MultipartFile doc2,      // offerLetter OR leavingLetter
            EmployeeExperience employeeExperience) {

        employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElse(new EmployeePersonalDetails());

        if (employeeExperience == EmployeeExperience.FRESHER) {
            FresherPersonalDetailsRequest req = parseJson(dataJson, FresherPersonalDetailsRequest.class);
            fillCommonFields(pd, req);
            pd.setUnaNumber(null);
            clearExperiencedFields(pd);
            if (aadhaarCard != null && !aadhaarCard.isEmpty())
                pd.setAadhaarDocPath(documentStorageService.save(aadhaarCard, "aadhaar", employeeId));
            if (doc1 != null && !doc1.isEmpty())
                pd.setTcDocPath(documentStorageService.save(doc1, "tc", employeeId));
            if (doc2 != null && !doc2.isEmpty())
                pd.setOfferLetterDocPath(documentStorageService.save(doc2, "offer-letter", employeeId));
        } else {
            ExperiencedPersonalDetailsRequest req = parseJson(dataJson, ExperiencedPersonalDetailsRequest.class);
            fillCommonFields(pd, req);
            pd.setUnaNumber(req.getUnaNumber());
            pd.setPreviousRole(req.getPreviousRole());
            pd.setOldCompanyName(req.getOldCompanyName());
            pd.setOldCompanyFromDate(req.getOldCompanyFromDate());
            pd.setOldCompanyEndDate(req.getOldCompanyEndDate());
            clearFresherFields(pd);
            if (aadhaarCard != null && !aadhaarCard.isEmpty())
                pd.setAadhaarDocPath(documentStorageService.save(aadhaarCard, "aadhaar", employeeId));
            if (doc1 != null && !doc1.isEmpty())
                pd.setExperienceCertDocPath(documentStorageService.save(doc1, "experience-cert", employeeId));
            if (doc2 != null && !doc2.isEmpty())
                pd.setLeavingLetterDocPath(documentStorageService.save(doc2, "leaving-letter", employeeId));
        }
        Employee employee = employeeRepository.findByEmpId(employeeId)
                        .orElseThrow(()-> new EntityNotFoundException("Employee entity not found"));
        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.VERIFIED);
        if (pd.getSubmittedAt() == null) pd.setSubmittedAt(LocalDateTime.now());

        return personalDetailsRepository.save(pd);
    }

    // ─── getPersonalDetails (HR/Admin view) ───────────────────────

    public EmployeePersonalDetails getPersonalDetails(String  employeeId) {
        return personalDetailsRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not yet submitted for employee: " + employeeId));
    }

    // ─── Unchanged methods ────────────────────────────────────────

    public Page<Employee> getAllEmployees(String name, String email, String role,
                                          String reportingId, Boolean active, Pageable pageable) {
        return employeeRepository.findAll(
                createSpecification(name, email, role, reportingId, active), pageable);
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee employee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
        if (employee.getName() != null) existing.setName(employee.getName());
        if (employee.getEmail() != null) existing.setEmail(employee.getEmail());
        if (employee.getRole() != null) existing.setRole(employee.getRole());
        if (employee.getReportingId() != null) existing.setReportingId(employee.getReportingId());
        return employeeRepository.save(existing);
    }

    @Transactional
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findByEmpId(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public List<Employee> getTeamMembers(String managerId) {
        return employeeRepository.findByReportingId(managerId);
    }

//    public List<Employee> getTeamLeaderMembers(Long teamLeaderId) {
//        return employeeRepository.findByTeamLeaderId(teamLeaderId);
//    }

    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByNameContainingIgnoreCase(query);
    }

    public Long getActiveEmployeesCount() {
        return employeeRepository.countByActive(true);
    }

    // ─── Private helpers ──────────────────────────────────────────

    /**
     * Fills all common text fields from either fresher or experienced request
     */
    private void fillCommonFields(EmployeePersonalDetails pd, Object request) {
        if (request instanceof FresherPersonalDetailsRequest r) {
            pd.setFirstName(r.getFirstName());
            pd.setLastName(r.getLastName());
            pd.setContactNumber(r.getContactNumber());
            pd.setGender(r.getGender());
            pd.setMaritalStatus(r.getMaritalStatus());
            pd.setAadharNumber(r.getAadharNumber());
            pd.setPersonalEmail(r.getPersonalEmail());
            pd.setDateOfBirth(r.getDateOfBirth());
            pd.setPresentAddress(r.getPresentAddress());
            pd.setPermanentAddress(r.getPermanentAddress());
            pd.setBloodGroup(r.getBloodGroup());
            pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
            pd.setDesignation(r.getDesignation());
            pd.setSkillSet(r.getSkillSet());
            pd.setAccountNumber(r.getAccountNumber());
            pd.setBankName(r.getBankName());
            pd.setFatherName(r.getFatherName());
            pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
            pd.setFatherOccupation(r.getFatherOccupation());
            pd.setFatherAlive(r.getFatherAlive());
            pd.setMotherName(r.getMotherName());
            pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
            pd.setMotherOccupation(r.getMotherOccupation());
            pd.setMotherAlive(r.getMotherAlive());
        } else if (request instanceof ExperiencedPersonalDetailsRequest r) {
            pd.setFirstName(r.getFirstName());
            pd.setLastName(r.getLastName());
            pd.setContactNumber(r.getContactNumber());
            pd.setGender(r.getGender());
            pd.setMaritalStatus(r.getMaritalStatus());
            pd.setAadharNumber(r.getAadharNumber());
            pd.setPersonalEmail(r.getPersonalEmail());
            pd.setDateOfBirth(r.getDateOfBirth());
            pd.setPresentAddress(r.getPresentAddress());
            pd.setPermanentAddress(r.getPermanentAddress());
            pd.setBloodGroup(r.getBloodGroup());
            pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
            pd.setDesignation(r.getDesignation());
            pd.setSkillSet(r.getSkillSet());
            pd.setAccountNumber(r.getAccountNumber());
            pd.setBankName(r.getBankName());
            pd.setFatherName(r.getFatherName());
            pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
            pd.setFatherOccupation(r.getFatherOccupation());
            pd.setFatherAlive(r.getFatherAlive());
            pd.setMotherName(r.getMotherName());
            pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
            pd.setMotherOccupation(r.getMotherOccupation());
            pd.setMotherAlive(r.getMotherAlive());
        }
    }

    private void clearFresherFields(EmployeePersonalDetails pd) {
        pd.setTcDocPath(null);
        pd.setOfferLetterDocPath(null);
    }

    private void clearExperiencedFields(EmployeePersonalDetails pd) {
        pd.setExperienceCertDocPath(null);
        pd.setLeavingLetterDocPath(null);
        pd.setPreviousRole(null);
        pd.setOldCompanyName(null);
        pd.setOldCompanyFromDate(null);
        pd.setOldCompanyEndDate(null);
    }

    private void deleteOldFresherDocs(EmployeePersonalDetails pd) {
        documentStorageService.delete(pd.getAadhaarDocPath());
        documentStorageService.delete(pd.getTcDocPath());
        documentStorageService.delete(pd.getOfferLetterDocPath());
    }

    private void deleteOldExperiencedDocs(EmployeePersonalDetails pd) {
        documentStorageService.delete(pd.getAadhaarDocPath());
        documentStorageService.delete(pd.getExperienceCertDocPath());
        documentStorageService.delete(pd.getLeavingLetterDocPath());
    }

    private void mapPersonalDetailsToResponse(EmployeePersonalDetails pd, ProfileResponse r) {
        r.setFirstName(pd.getFirstName());
        r.setLastName(pd.getLastName());
        r.setContactNumber(pd.getContactNumber());
        r.setGender(pd.getGender());
        r.setMaritalStatus(pd.getMaritalStatus());
        r.setAadharNumber(pd.getAadharNumber());
        r.setAadhaarDocPath(pd.getAadhaarDocPath());
        r.setPersonalEmail(pd.getPersonalEmail());
        r.setDateOfBirth(pd.getDateOfBirth());
        r.setPresentAddress(pd.getPresentAddress());
        r.setPermanentAddress(pd.getPermanentAddress());
        r.setBloodGroup(pd.getBloodGroup());
        r.setEmergencyContactNumber(pd.getEmergencyContactNumber());
        r.setFatherName(pd.getFatherName());
        r.setMotherName(pd.getMotherName());
        r.setDesignation(pd.getDesignation());
        r.setAccountNumber(pd.getAccountNumber());
        r.setBankName(pd.getBankName());
        r.setPfNumber(pd.getPfNumber());
        r.setUnaNumber(pd.getUnaNumber());

        if (pd.getSkillSet() != null && !pd.getSkillSet().isBlank()) {
            r.setSkillSet(Arrays.stream(pd.getSkillSet().split(","))
                    .map(String::trim).collect(Collectors.toList()));
        }

        if (pd.getEmployee().getEmployeeExperience() == EmployeeExperience.FRESHER) {
            r.setTcDocPath(pd.getTcDocPath());
            r.setOfferLetterDocPath(pd.getOfferLetterDocPath());
        } else if (pd.getEmployee().getEmployeeExperience() == EmployeeExperience.EXPERIENCED) {
            r.setExperienceCertDocPath(pd.getExperienceCertDocPath());
            r.setLeavingLetterDocPath(pd.getLeavingLetterDocPath());
            r.setPreviousRole(pd.getPreviousRole());
            r.setOldCompanyName(pd.getOldCompanyName());
            r.setOldCompanyFromDate(pd.getOldCompanyFromDate());
            r.setOldCompanyEndDate(pd.getOldCompanyEndDate());
        }
    }

    private void notifyHr(String employeeName, String employeeId) {
        List<Employee> hrEmployees = employeeRepository.findAllByRoleName("HR");

        if (hrEmployees.isEmpty()) {
//            log.warn("No active HR employees found to notify for submission by employee: {}", employeeId);
            return;
        }

        String msg = "Employee " + employeeName + " (ID: " + employeeId
                + ") has submitted their profile. Please review and verify.";

        for (Employee hr : hrEmployees) {
            userRepository.findByEmployee_EmpId(hr.getEmpId()).ifPresent(hrUser ->
                    notificationService.createNotification(
                            hr.getEmpId(),                  // recipientId  → String
                            "noreply@company.com",           // senderEmail
                            hrUser.getEmail(),               // recipientEmail
                            EventType.PROFILE_SUBMITTED,
                            Channel.EMAIL,
                            msg)
            );
        }
    }

    private void notifyEmployee(Employee employee, EventType eventType, String message) {
        User empUser = userRepository.findByEmployee_EmpId(employee.getEmpId()).orElse(null);
        if (empUser == null) return;
        notificationService.createNotification(
                employee.getEmpId(), "noreply@company.com", empUser.getEmail(),
                eventType, Channel.EMAIL, message);
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty())
            throw new BadRequestException(fieldName + " document is required.");
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON for personal details: " + e.getMessage());
        }
    }

    private Specification<Employee> createSpecification(String name, String email,
                                                        String role, String reportingId,
                                                        Boolean active) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (name != null && !name.isEmpty())
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"));

            if (email != null && !email.isEmpty())
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));

            if (role != null && !role.isEmpty()) {
                // Role is a ManyToOne entity — join and filter by role.name
                var roleJoin = root.join("role", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(
                        cb.lower(roleJoin.get("name")),
                        role.toLowerCase()));
            }

            if (reportingId != null && !reportingId.isEmpty())
                predicates.add(cb.equal(root.get("reportingId"), reportingId));

            if (active != null)
                predicates.add(cb.equal(root.get("active"), active));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

//    public List<Employee> getOnboardingPending() {
//        return employeeRepository.findOnboardingPending();
//    }
//
//    public void decideBio(Long employeeId, BiometricVpnStatus decision) {
//        Employee employee = employeeRepository.findById(employeeId)
//                .orElseThrow(() -> new RuntimeException("Employee not Found"));
//        if (employee.getVpnStatus() == BiometricVpnStatus.PROVIDED) {
//            employee.setOnboardingCompletedAt(LocalDateTime.now());
//        }
//        employee.setBiometricStatus(decision);
//        employeeRepository.save(employee);
//    }

    public void decideVpn(String employeeId, BiometricVpnStatus decision) {
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not Found"));
        if (employee.getOnboarding().getBiometricStatus() == BiometricVpnStatus.PROVIDED) {
            EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                    .orElseThrow(()-> new EntityNotFoundException("Onboarding not found"));
            eo.setBiometricStatus(decision);
            employeeOnboardingRepository.save(eo);
        }
        EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(()-> new EntityNotFoundException("Onboarding not found"));
        eo.setVpnStatus(decision);
        employeeOnboardingRepository.save(eo);
        employeeRepository.save(employee);
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

}