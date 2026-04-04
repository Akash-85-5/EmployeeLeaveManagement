package com.emp_management.feature.employee.service;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.employee.dto.*;
import com.emp_management.feature.employee.entity.*;
import com.emp_management.feature.employee.mapper.EmployeeMapper;
import com.emp_management.feature.employee.repository.EmployeeOnboardingRepository;
import com.emp_management.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.feature.notification.service.NotificationService;
import com.emp_management.infrastructure.storage.DocumentStorageService;
import com.emp_management.shared.dto.BranchListDto;
import com.emp_management.shared.dto.EmployeeListDto;
import com.emp_management.shared.entity.Department;
import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.*;
import com.emp_management.shared.exceptions.BadRequestException;
import com.emp_management.shared.repository.BranchRepository;
import com.emp_management.shared.repository.DepartmentRepository;
import com.emp_management.shared.repository.RoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final NotificationService notificationService;
    private final LeaveAllocationService leaveAllocationService;
    private final DocumentStorageService documentStorageService;
    private final EmployeeOnboardingRepository employeeOnboardingRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           EmployeePersonalDetailsRepository personalDetailsRepository,
                           NotificationService notificationService,
                           LeaveAllocationService leaveAllocationService,
                           DocumentStorageService documentStorageService,
                           EmployeeOnboardingRepository employeeOnboardingRepository,
                           DepartmentRepository departmentRepository,
                           RoleRepository roleRepository,
                           BranchRepository branchRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
        this.notificationService = notificationService;
        this.leaveAllocationService = leaveAllocationService;
        this.documentStorageService = documentStorageService;
        this.employeeOnboardingRepository = employeeOnboardingRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
    }

    // ─── Lookups ───────────────────────────────────────────────────

    public NameDto getEmployeeName(String empId) {
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        NameDto name = new NameDto();
        name.setEmpId(employee.getEmpId());
        name.setEmpName(employee.getName());
        return name;
    }

    public List<Department> getDepartmentList() { return departmentRepository.findAll(); }
    public List<Role> getRoleList() { return roleRepository.findAll(); }
    public List<EmployeeListDto> getAllEmployees() { return employeeRepository.findAllActiveEmployeeBasicDetails(); }
    public List<BranchListDto> getAllBranches() { return branchRepository.findAllBranchDetails(); }

    // ─── Profile ───────────────────────────────────────────────────

    public ProfileResponse getProfile(String employeeId) {
        User user = userRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        ProfileResponse response = new ProfileResponse();
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

        Optional<EmployeePersonalDetails> personalOpt =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        if (personalOpt.isPresent()) {
            EmployeePersonalDetails pd = personalOpt.get();
            mapPersonalDetailsToResponse(pd, response);
            response.setPersonalDetailsComplete(true);
            response.setPersonalDetailsLocked(pd.isLocked());
            response.setVerificationStatus(pd.getVerificationStatus());
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

    // ─────────────────────────────────────────────────────────────
    // FRESHER — public API
    // ─────────────────────────────────────────────────────────────

    /**
     * POST: first-time submission.
     * Allowed states: no record yet, OR existing record is REJECTED.
     */
    @Transactional
    public EmployeePersonalDetails submitFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        // POST allows: no existing record, OR existing is REJECTED
        existing.ifPresent(pd -> guardNotPendingOrVerified(pd.getVerificationStatus()));

        return saveFresherDetails(employeeId, dataJson,
                idProof, tenthMarksheet, twelfthMarksheet,
                degreeCertificate, offerLetter, passportPhoto, existing);
    }

    /**
     * PUT: resubmission after HR rejection only.
     * Fails if there is no existing record or status is not REJECTED.
     */
    @Transactional
    public EmployeePersonalDetails updateFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        // PUT requires an existing record that is specifically REJECTED
        EmployeePersonalDetails pd = existing.orElseThrow(() ->
                new BadRequestException("No personal details found to update for employee: " + employeeId));
//        guardOnlyRejected(pd.getVerificationStatus());

        return saveFresherDetails(employeeId, dataJson,
                idProof, tenthMarksheet, twelfthMarksheet,
                degreeCertificate, offerLetter, passportPhoto, existing);
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCED — public API
    // ─────────────────────────────────────────────────────────────

    /**
     * POST: first-time submission.
     * Allowed states: no record yet, OR existing record is REJECTED.
     */
    @Transactional
    public EmployeePersonalDetails submitExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        // POST allows: no existing record, OR existing is REJECTED
        existing.ifPresent(pd -> guardNotPendingOrVerified(pd.getVerificationStatus()));

        return saveExperiencedDetails(employeeId, dataJson,
                idProof, passportPhoto, experienceCerts, relievingLetter, existing);
    }

    /**
     * PUT: resubmission after HR rejection only.
     * Fails if there is no existing record or status is not REJECTED.
     */
    @Transactional
    public EmployeePersonalDetails updateExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        // PUT requires an existing record that is specifically REJECTED
        EmployeePersonalDetails pd = existing.orElseThrow(() ->
                new BadRequestException("No personal details found to update for employee: " + employeeId));
//        guardOnlyRejected(pd.getVerificationStatus());

        return saveExperiencedDetails(employeeId, dataJson,
                idProof, passportPhoto, experienceCerts, relievingLetter, existing);
    }

    // ─────────────────────────────────────────────────────────────
    // Shared internal save logic
    // ─────────────────────────────────────────────────────────────

    /**
     * Core fresher save — used by both POST and PUT.
     * By the time this is called, the state guard has already passed.
     */
    private EmployeePersonalDetails saveFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        FresherPersonalDetailsRequest request =
                parseJson(dataJson, FresherPersonalDetailsRequest.class);

        // Validate all 6 documents
        validateFile(idProof,           "ID Proof");
        validateFile(tenthMarksheet,    "10th Marksheet");
        validateFile(twelfthMarksheet,  "12th Marksheet");
        validateFile(degreeCertificate, "Degree / Provisional Certificate");
        validateFile(offerLetter,       "Offer Letter");
        validateFile(passportPhoto,     "Passport-size Photo");

        // Validate spouse fields when MARRIED
        validateSpouse(request.getMaritalStatus(),
                request.getSpouseName(),
                request.getSpouseDateOfBirth(),
                request.getSpouseOccupation(),
                request.getSpouseContactNumber());

        // Delete old files from disk before overwriting (only if resubmitting)
        existing.ifPresent(pd -> deleteFresherDocFiles(pd.getFresherDocument()));

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());

        fillCommonFields(pd, request);
        pd.setUanNumber(null);
        replaceChildren(pd, request.getChildren());

        // Build / reuse the FresherDocument row
        FresherDocument doc = Optional.ofNullable(pd.getFresherDocument())
                .orElse(new FresherDocument());
        doc.setIdProofPath(documentStorageService.save(idProof,           "id-proof",           employeeId));
        doc.setTenthMarksheetPath(documentStorageService.save(tenthMarksheet,    "10th-marksheet",     employeeId));
        doc.setTwelfthMarksheetPath(documentStorageService.save(twelfthMarksheet,  "12th-marksheet",     employeeId));
        doc.setDegreeCertificatePath(documentStorageService.save(degreeCertificate, "degree-certificate", employeeId));
        doc.setOfferLetterPath(documentStorageService.save(offerLetter,       "offer-letter",       employeeId));
        doc.setPassportPhotoPath(documentStorageService.save(passportPhoto,     "passport-photo",     employeeId));
        doc.setPersonalDetails(pd);
        pd.setFresherDocument(doc);

        // Clear any lingering experienced-document rows
        clearExperiencedDocEntities(pd);

        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.PENDING);
        pd.setHrRemarks(null);
        pd.setSubmittedAt(LocalDateTime.now());

        EmployeePersonalDetails saved = personalDetailsRepository.save(pd);
        leaveAllocationService.allocateForNewEmployee(employeeId);
        notifyHr(employee.getName(), employeeId);
        return saved;
    }

    /**
     * Core experienced save — used by both POST and PUT.
     * By the time this is called, the state guard has already passed.
     */
    private EmployeePersonalDetails saveExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        ExperiencedPersonalDetailsRequest request =
                parseJson(dataJson, ExperiencedPersonalDetailsRequest.class);

        // Validate single files
        validateFile(idProof,         "ID Proof");
        validateFile(passportPhoto,   "Passport-size Photo");
        validateFile(relievingLetter, "Relieving Letter");

        // Validate experience cert list
        if (experienceCerts == null || experienceCerts.isEmpty())
            throw new BadRequestException("At least one experience certificate is required.");

        List<ExperienceEntryDto> experiences = request.getExperiences();
        if (experiences == null || experiences.isEmpty())
            throw new BadRequestException("At least one experience entry is required.");

        if (experienceCerts.size() != experiences.size())
            throw new BadRequestException(
                    "Number of experience certificate files (" + experienceCerts.size()
                            + ") must match number of experience entries (" + experiences.size() + ").");

        for (int i = 0; i < experienceCerts.size(); i++)
            validateFile(experienceCerts.get(i), "Experience certificate for entry " + (i + 1));

        // Exactly one entry must be the last company
        long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
        if (lastCount != 1)
            throw new BadRequestException(
                    "Exactly one experience entry must be marked as the last company.");

        // Validate date ranges
        for (int i = 0; i < experiences.size(); i++) {
            ExperienceEntryDto e = experiences.get(i);
            if (e.getFromDate() != null && e.getEndDate() != null
                    && !e.getFromDate().isBefore(e.getEndDate()))
                throw new BadRequestException(
                        "Experience entry " + (i + 1) + ": fromDate must be before endDate.");
        }

        // Validate spouse fields when MARRIED
        validateSpouse(request.getMaritalStatus(),
                request.getSpouseName(),
                request.getSpouseDateOfBirth(),
                request.getSpouseOccupation(),
                request.getSpouseContactNumber());

        // Delete old files from disk (only if resubmitting)
        existing.ifPresent(pd -> deleteExperiencedDocFiles(pd.getExperiencedDocuments()));

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());

        fillCommonFields(pd, request);
        pd.setUanNumber(request.getUanNumber());
        replaceChildren(pd, request.getChildren());

        // Clear fresher document and old experienced rows
        pd.setFresherDocument(null);
        pd.getExperiencedDocuments().clear();

        // Save shared files once
        String idProofPath       = documentStorageService.save(idProof,       "id-proof",       employeeId);
        String passportPhotoPath = documentStorageService.save(passportPhoto,  "passport-photo", employeeId);

        for (int i = 0; i < experiences.size(); i++) {
            ExperienceEntryDto entry = experiences.get(i);
            ExperiencedDocument doc = new ExperiencedDocument();
            doc.setPersonalDetails(pd);
            doc.setCompanyName(entry.getCompanyName());
            doc.setRole(entry.getRole());
            doc.setFromDate(entry.getFromDate());
            doc.setEndDate(entry.getEndDate());
            doc.setExperienceCertPath(
                    documentStorageService.save(experienceCerts.get(i), "experience-cert", employeeId));

            // idProof and passportPhoto attached to first entry only
            if (i == 0) {
                doc.setIdProofPath(idProofPath);
                doc.setPassportPhotoPath(passportPhotoPath);
            }

            // Relieving letter attached to the last-company entry
            if (entry.isLastCompany()) {
                doc.setRelievingLetterPath(
                        documentStorageService.save(relievingLetter, "relieving-letter", employeeId));
            }

            pd.getExperiencedDocuments().add(doc);
        }

        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.PENDING);
        pd.setHrRemarks(null);
        pd.setSubmittedAt(LocalDateTime.now());

        EmployeePersonalDetails saved = personalDetailsRepository.save(pd);
        leaveAllocationService.allocateForNewEmployee(employeeId);
        notifyHr(employee.getName(), employeeId);
        return saved;
    }

    // ─── HR: verify / reject ───────────────────────────────────────

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
            pd.setLocked(false);
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

    // ─── Admin: set PF number ──────────────────────────────────────

    @Transactional
    public EmployeePersonalDetails updatePfNumber(String employeeId, PfUpdateRequest request) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not found for employee: " + employeeId));
        pd.setPfNumber(request.getPfNumber());
        return personalDetailsRepository.save(pd);
    }

    // ─── HR/Admin: get personal details ───────────────────────────

    public EmployeePersonalDetails getPersonalDetails(String employeeId) {
        return personalDetailsRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not yet submitted for employee: " + employeeId));
    }

    public List<EmployeePersonalDetails> getPendingVerifications() {
        return personalDetailsRepository.findByVerificationStatus(VerificationStatus.PENDING);
    }

    public List<EmployeePersonalDetails> getAllVerifications() {
        return personalDetailsRepository.findAllByOrderBySubmittedAtDesc();
    }

    // ─── Employee list / search ────────────────────────────────────

    public Page<EmployeeResponseDTO> getAllEmployees(String name, String email, String role,
                                                     String reportingId, Boolean active,
                                                     Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(
                createSpecification(name, email, role, reportingId, active), pageable);
        return page.map(EmployeeMapper::toDTO);
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

    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByNameContainingIgnoreCase(query);
    }

    public Long getActiveEmployeesCount() {
        return employeeRepository.countByActive(true);
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }

    public void decideVpn(String employeeId, BiometricVpnStatus decision) {
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not Found"));
        if (employee.getOnboarding().getBiometricStatus() == BiometricVpnStatus.PROVIDED) {
            EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                    .orElseThrow(() -> new EntityNotFoundException("Onboarding not found"));
            eo.setBiometricStatus(decision);
            employeeOnboardingRepository.save(eo);
        }
        EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Onboarding not found"));
        eo.setVpnStatus(decision);
        employeeOnboardingRepository.save(eo);
        employeeRepository.save(employee);
    }

    // ─────────────────────────────────────────────────────────────
    // State guards
    // ─────────────────────────────────────────────────────────────

    /**
     * Used by POST: blocks if the existing record is PENDING or VERIFIED.
     * REJECTED is allowed (employee is correcting and resubmitting).
     */
    private void guardNotPendingOrVerified(VerificationStatus status) {
        if (status == VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Your profile is already submitted and pending HR verification.");
        if (status == VerificationStatus.VERIFIED)
            throw new BadRequestException(
                    "Your profile is already verified. Contact Admin/HR for updates.");
    }

    /**
     * Used by PUT: only REJECTED status allows an update.
     * PENDING and VERIFIED both block the update.
     */
    private void guardOnlyRejected(VerificationStatus status) {
        if (status == VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Cannot edit while your profile is pending HR verification.");
        if (status == VerificationStatus.VERIFIED)
            throw new BadRequestException(
                    "Cannot edit a verified profile. Contact Admin/HR for updates.");
        // REJECTED → allowed
    }

    // ─────────────────────────────────────────────────────────────
    // Field-fill helpers
    // ─────────────────────────────────────────────────────────────

    private void fillCommonFields(EmployeePersonalDetails pd, FresherPersonalDetailsRequest r) {
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
        pd.setIfscCode(r.getIfscCode());
        pd.setBankBranchName(r.getBankBranchName());
        pd.setFatherName(r.getFatherName());
        pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        pd.setFatherOccupation(r.getFatherOccupation());
        pd.setFatherAlive(r.getFatherAlive());
        pd.setMotherName(r.getMotherName());
        pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        pd.setMotherOccupation(r.getMotherOccupation());
        pd.setMotherAlive(r.getMotherAlive());
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
            pd.setSpouseName(r.getSpouseName());
            pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            pd.setSpouseOccupation(r.getSpouseOccupation());
            pd.setSpouseContactNumber(r.getSpouseContactNumber());
        } else {
            pd.setSpouseName(null);
            pd.setSpouseDateOfBirth(null);
            pd.setSpouseOccupation(null);
            pd.setSpouseContactNumber(null);
        }
    }

    private void fillCommonFields(EmployeePersonalDetails pd, ExperiencedPersonalDetailsRequest r) {
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
        pd.setIfscCode(r.getIfscCode());
        pd.setBankBranchName(r.getBankBranchName());
        pd.setFatherName(r.getFatherName());
        pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        pd.setFatherOccupation(r.getFatherOccupation());
        pd.setFatherAlive(r.getFatherAlive());
        pd.setMotherName(r.getMotherName());
        pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        pd.setMotherOccupation(r.getMotherOccupation());
        pd.setMotherAlive(r.getMotherAlive());
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
            pd.setSpouseName(r.getSpouseName());
            pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            pd.setSpouseOccupation(r.getSpouseOccupation());
            pd.setSpouseContactNumber(r.getSpouseContactNumber());
        } else {
            pd.setSpouseName(null);
            pd.setSpouseDateOfBirth(null);
            pd.setSpouseOccupation(null);
            pd.setSpouseContactNumber(null);
        }
    }

    private void replaceChildren(EmployeePersonalDetails pd, List<ChildDto> childDtos) {
        pd.getChildren().clear();
        if (childDtos == null || childDtos.isEmpty()) return;
        for (ChildDto dto : childDtos) {
            EmployeeChild child = new EmployeeChild();
            child.setChildName(dto.getChildName());
            child.setGender(dto.getGender());
            child.setAge(dto.getAge());
            child.setPersonalDetails(pd);
            pd.getChildren().add(child);
        }
    }

    private void clearExperiencedDocEntities(EmployeePersonalDetails pd) {
        pd.getExperiencedDocuments().clear();
    }

    // ─────────────────────────────────────────────────────────────
    // Disk-file deletion helpers
    // ─────────────────────────────────────────────────────────────

    private void deleteFresherDocFiles(FresherDocument doc) {
        if (doc == null) return;
        documentStorageService.delete(doc.getIdProofPath());
        documentStorageService.delete(doc.getTenthMarksheetPath());
        documentStorageService.delete(doc.getTwelfthMarksheetPath());
        documentStorageService.delete(doc.getDegreeCertificatePath());
        documentStorageService.delete(doc.getOfferLetterPath());
        documentStorageService.delete(doc.getPassportPhotoPath());
    }

    private void deleteExperiencedDocFiles(List<ExperiencedDocument> docs) {
        if (docs == null) return;
        for (ExperiencedDocument doc : docs) {
            documentStorageService.delete(doc.getIdProofPath());
            documentStorageService.delete(doc.getPassportPhotoPath());
            documentStorageService.delete(doc.getExperienceCertPath());
            documentStorageService.delete(doc.getRelievingLetterPath());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Profile response mapping
    // ─────────────────────────────────────────────────────────────

    private void mapPersonalDetailsToResponse(EmployeePersonalDetails pd, ProfileResponse r) {
        r.setFirstName(pd.getFirstName());
        r.setLastName(pd.getLastName());
        r.setContactNumber(pd.getContactNumber());
        r.setGender(pd.getGender());
        r.setMaritalStatus(pd.getMaritalStatus());
        r.setAadharNumber(pd.getAadharNumber());
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
        r.setIfscCode(pd.getIfscCode());
        r.setBankBranchName(pd.getBankBranchName());
        r.setPfNumber(pd.getPfNumber());
        r.setUanNumber(pd.getUanNumber());
        r.setSpouseName(pd.getSpouseName());
        r.setSpouseDateOfBirth(pd.getSpouseDateOfBirth());
        r.setSpouseOccupation(pd.getSpouseOccupation());
        r.setSpouseContactNumber(pd.getSpouseContactNumber());

        if (pd.getSkillSet() != null && !pd.getSkillSet().isBlank()) {
            r.setSkillSet(Arrays.stream(pd.getSkillSet().split(","))
                    .map(String::trim).collect(Collectors.toList()));
        }

        if (pd.getChildren() != null) {
            r.setChildren(pd.getChildren().stream().map(c -> {
                ChildDto dto = new ChildDto();
                dto.setChildName(c.getChildName());
                dto.setGender(c.getGender());
                dto.setAge(c.getAge());
                return dto;
            }).collect(Collectors.toList()));
        }

        if (pd.getEmployee().getEmployeeExperience() == EmployeeExperience.FRESHER) {
            FresherDocument doc = pd.getFresherDocument();
            if (doc != null) {
                r.setIdProofPath(doc.getIdProofPath());
                r.setTenthMarksheetPath(doc.getTenthMarksheetPath());
                r.setTwelfthMarksheetPath(doc.getTwelfthMarksheetPath());
                r.setDegreeCertificatePath(doc.getDegreeCertificatePath());
                r.setOfferLetterPath(doc.getOfferLetterPath());
                r.setPassportPhotoPath(doc.getPassportPhotoPath());
            }
        } else if (pd.getEmployee().getEmployeeExperience() == EmployeeExperience.EXPERIENCED) {
            r.setExperiencedDocuments(pd.getExperiencedDocuments());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Notification helpers
    // ─────────────────────────────────────────────────────────────

    private void notifyHr(String employeeName, String employeeId) {
        List<Employee> hrEmployees = employeeRepository.findAllByRoleName("HR");
        if (hrEmployees.isEmpty()) return;
        String msg = "Employee " + employeeName + " (ID: " + employeeId
                + ") has submitted their profile. Please review and verify.";
        for (Employee hr : hrEmployees) {
            userRepository.findByEmployee_EmpId(hr.getEmpId()).ifPresent(hrUser ->
                    notificationService.createNotification(
                            hr.getEmpId(), hr.getEmail(), hrUser.getEmail(),
                            EventType.PROFILE_SUBMITTED, Channel.EMAIL, msg));
        }
    }

    private void notifyEmployee(Employee employee, EventType eventType, String message) {
        User empUser = userRepository.findByEmployee_EmpId(employee.getEmpId()).orElse(null);
        if (empUser == null) return;
        notificationService.createNotification(
                employee.getEmpId(), "info@wenxttech.com", empUser.getEmail(),
                eventType, Channel.EMAIL, message);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────

    private void validateSpouse(MaritalStatus status,
                                String spouseName,
                                LocalDate spouseDateOfBirth,
                                String spouseOccupation,
                                String spouseContactNumber) {
        if (status == MaritalStatus.MARRIED) {
            if (spouseName == null || spouseName.isBlank())
                throw new BadRequestException("Spouse name is required for married employees.");
            if (spouseDateOfBirth == null)
                throw new BadRequestException("Spouse date of birth is required for married employees.");
            if (spouseOccupation == null || spouseOccupation.isBlank())
                throw new BadRequestException("Spouse occupation is required for married employees.");
            if (spouseContactNumber == null || spouseContactNumber.isBlank())
                throw new BadRequestException("Spouse contact number is required for married employees.");
        }
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
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (email != null && !email.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            if (role != null && !role.isEmpty()) {
                var roleJoin = root.join("role", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(cb.lower(roleJoin.get("name")), role.toLowerCase()));
            }
            if (reportingId != null && !reportingId.isEmpty())
                predicates.add(cb.equal(root.get("reportingId"), reportingId));
            if (active != null)
                predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}