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

        ProfileResponse response = buildBaseProfile(employee, user);

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

    public ProfileResponse getPersonalDetailsAsProfile(String employeeId) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not yet submitted for employee: " + employeeId));

        User user = userRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ProfileResponse response = buildBaseProfile(pd.getEmployee(), user);
        mapPersonalDetailsToResponse(pd, response);
        response.setPersonalDetailsComplete(true);
        response.setPersonalDetailsLocked(pd.isLocked());
        response.setVerificationStatus(pd.getVerificationStatus());
        if (pd.getVerificationStatus() == VerificationStatus.REJECTED) {
            response.setHrRemarks(pd.getHrRemarks());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // FRESHER — POST (full submission, all fields + all files required)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void submitFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);
        existing.ifPresent(pd -> guardNotPendingOrVerified(pd.getVerificationStatus()));

        saveFresherDetails(employeeId, dataJson,
                idProof, tenthMarksheet, twelfthMarksheet,
                degreeCertificate, offerLetter, passportPhoto, existing);
    }

    // ─────────────────────────────────────────────────────────────
    // FRESHER — PUT (partial update, only changed fields/files)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void updateFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto) {

        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "No personal details found to update for employee: " + employeeId));

        // guardOnlyRejected(pd.getVerificationStatus()); // Uncomment when HR-rejection gate needed

        FresherUpdateRequest request = parseJson(dataJson, FresherUpdateRequest.class);

        // Patch only non-null text fields
        patchCommonFields(pd, request);

        // Patch files: only replace if a new file was actually sent
        FresherDocument doc = Optional.ofNullable(pd.getFresherDocument())
                .orElse(new FresherDocument());

        if (hasFile(idProof))
            doc.setIdProofPath(documentStorageService.save(idProof, "id-proof", employeeId));
        if (hasFile(tenthMarksheet))
            doc.setTenthMarksheetPath(documentStorageService.save(tenthMarksheet, "10th-marksheet", employeeId));
        if (hasFile(twelfthMarksheet))
            doc.setTwelfthMarksheetPath(documentStorageService.save(twelfthMarksheet, "12th-marksheet", employeeId));
        if (hasFile(degreeCertificate))
            doc.setDegreeCertificatePath(documentStorageService.save(degreeCertificate, "degree-certificate", employeeId));
        if (hasFile(offerLetter))
            doc.setOfferLetterPath(documentStorageService.save(offerLetter, "offer-letter", employeeId));
        if (hasFile(passportPhoto))
            doc.setPassportPhotoPath(documentStorageService.save(passportPhoto, "passport-photo", employeeId));

        doc.setPersonalDetails(pd);
        pd.setFresherDocument(doc);

        // Children: only replace if explicitly sent (even as empty list)
        if (request.getChildren() != null) {
            replaceChildren(pd, request.getChildren());
        }

        personalDetailsRepository.save(pd);
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCED — POST (full submission, all fields + files required)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void submitExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);
        existing.ifPresent(pd -> guardNotPendingOrVerified(pd.getVerificationStatus()));

        saveExperiencedDetails(employeeId, dataJson,
                idProof, passportPhoto, experienceCerts, relievingLetter, existing);
    }

    // ─────────────────────────────────────────────────────────────
    // EXPERIENCED — PUT (partial update)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void updateExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter) {

        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "No personal details found to update for employee: " + employeeId));

        // guardOnlyRejected(pd.getVerificationStatus()); // Uncomment when HR-rejection gate needed

        ExperiencedUpdateRequest request = parseJson(dataJson, ExperiencedUpdateRequest.class);

        // Patch only non-null text fields
        patchCommonFields(pd, request);

        // UAN — only update if sent
        if (request.getUanNumber() != null && !request.getUanNumber().isBlank()) {
            pd.setUanNumber(request.getUanNumber());
        }

        // Children: only replace if explicitly sent
        if (request.getChildren() != null) {
            replaceChildren(pd, request.getChildren());
        }

        // ── Experience entries: only replace if "experiences" is in the JSON ──
        List<ExperienceEntryDto> experiences = request.getExperiences();
        if (experiences != null) {
            // Experiences were explicitly sent — validate and fully replace
            if (experiences.isEmpty())
                throw new BadRequestException("Experience entries cannot be empty. Send at least one entry.");

            boolean hasCerts = experienceCerts != null && !experienceCerts.isEmpty();
            if (!hasCerts)
                throw new BadRequestException(
                        "experienceCerts files must be provided when updating experience entries.");

            if (experienceCerts.size() != experiences.size())
                throw new BadRequestException(
                        "Number of experience certificate files (" + experienceCerts.size()
                                + ") must match number of experience entries (" + experiences.size() + ").");

            for (int i = 0; i < experienceCerts.size(); i++)
                validateFile(experienceCerts.get(i), "Experience certificate for entry " + (i + 1));

            long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
            if (lastCount != 1)
                throw new BadRequestException(
                        "Exactly one experience entry must be marked as the last company.");

            boolean anyLastCompany = experiences.stream().anyMatch(ExperienceEntryDto::isLastCompany);
            if (anyLastCompany && !hasFile(relievingLetter))
                throw new BadRequestException(
                        "Relieving letter must be provided for the last company entry.");

            for (int i = 0; i < experiences.size(); i++) {
                ExperienceEntryDto e = experiences.get(i);
                if (e.getFromDate() != null && e.getEndDate() != null
                        && !e.getFromDate().isBefore(e.getEndDate()))
                    throw new BadRequestException(
                            "Experience entry " + (i + 1) + ": fromDate must be before endDate.");
            }

            // Delete old experience doc files from disk before replacing
            deleteExperiencedDocFiles(pd.getExperiencedDocuments());
            pd.getExperiencedDocuments().clear();

            // Find existing idProofPath and passportPhotoPath to carry forward if no new files sent
            String existingIdProofPath     = null;
            String existingPassportPath    = null;
            // (They were stored on the first entry of the old list — already cleared above,
            //  but we read them before clearing via the deleted docs — handled below)

            String idProofPath      = hasFile(idProof)
                    ? documentStorageService.save(idProof,      "id-proof",       employeeId)
                    : null; // will stay null on non-first entries anyway
            String passportPhotoPath = hasFile(passportPhoto)
                    ? documentStorageService.save(passportPhoto, "passport-photo", employeeId)
                    : null;

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
                if (i == 0) {
                    doc.setIdProofPath(idProofPath);       // null if no new file — acceptable
                    doc.setPassportPhotoPath(passportPhotoPath);
                }
                if (entry.isLastCompany()) {
                    doc.setRelievingLetterPath(
                            documentStorageService.save(relievingLetter, "relieving-letter", employeeId));
                }
                pd.getExperiencedDocuments().add(doc);
            }
        } else {
            // Experiences not sent — only update idProof/passportPhoto files on first entry if provided
            if (!pd.getExperiencedDocuments().isEmpty()) {
                ExperiencedDocument firstDoc = pd.getExperiencedDocuments().get(0);
                if (hasFile(idProof))
                    firstDoc.setIdProofPath(documentStorageService.save(idProof, "id-proof", employeeId));
                if (hasFile(passportPhoto))
                    firstDoc.setPassportPhotoPath(documentStorageService.save(passportPhoto, "passport-photo", employeeId));
            }
        }

        personalDetailsRepository.save(pd);
    }

    // ─────────────────────────────────────────────────────────────
    // Shared full-save logic (used by POST only)
    // ─────────────────────────────────────────────────────────────

    private void saveFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        FresherPersonalDetailsRequest request =
                parseJson(dataJson, FresherPersonalDetailsRequest.class);

        validateFile(idProof,           "ID Proof");
        validateFile(tenthMarksheet,    "10th Marksheet");
        validateFile(twelfthMarksheet,  "12th Marksheet");
        validateFile(degreeCertificate, "Degree / Provisional Certificate");
        validateFile(offerLetter,       "Offer Letter");
        validateFile(passportPhoto,     "Passport-size Photo");

        validateSpouseForFullSubmit(request.getMaritalStatus(),
                request.getSpouseName(), request.getSpouseDateOfBirth(),
                request.getSpouseOccupation(), request.getSpouseContactNumber());

        existing.ifPresent(pd -> deleteFresherDocFiles(pd.getFresherDocument()));

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());
        fillAllCommonFields(pd, request);
        pd.setUanNumber(null);
        replaceChildren(pd, request.getChildren());

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
        pd.getExperiencedDocuments().clear();

        pd.setEmployee(employee);
        pd.setLocked(true);
        pd.setVerificationStatus(VerificationStatus.PENDING);
        pd.setHrRemarks(null);
        pd.setSubmittedAt(LocalDateTime.now());

        personalDetailsRepository.save(pd);
        leaveAllocationService.allocateForNewEmployee(employeeId);
        notifyHr(employee.getName(), employeeId);
    }

    private void saveExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts, MultipartFile relievingLetter,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        ExperiencedPersonalDetailsRequest request =
                parseJson(dataJson, ExperiencedPersonalDetailsRequest.class);

        validateFile(idProof,         "ID Proof");
        validateFile(passportPhoto,   "Passport-size Photo");
        validateFile(relievingLetter, "Relieving Letter");

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

        long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
        if (lastCount != 1)
            throw new BadRequestException("Exactly one experience entry must be marked as the last company.");

        for (int i = 0; i < experiences.size(); i++) {
            ExperienceEntryDto e = experiences.get(i);
            if (e.getFromDate() != null && e.getEndDate() != null
                    && !e.getFromDate().isBefore(e.getEndDate()))
                throw new BadRequestException(
                        "Experience entry " + (i + 1) + ": fromDate must be before endDate.");
        }

        validateSpouseForFullSubmit(request.getMaritalStatus(),
                request.getSpouseName(), request.getSpouseDateOfBirth(),
                request.getSpouseOccupation(), request.getSpouseContactNumber());

        existing.ifPresent(pd -> deleteExperiencedDocFiles(pd.getExperiencedDocuments()));

        EmployeePersonalDetails pd = existing.orElse(new EmployeePersonalDetails());
        fillAllCommonFields(pd, request);
        pd.setUanNumber(request.getUanNumber());
        replaceChildren(pd, request.getChildren());

        pd.setFresherDocument(null);
        pd.getExperiencedDocuments().clear();

        String idProofPath       = documentStorageService.save(idProof,      "id-proof",       employeeId);
        String passportPhotoPath = documentStorageService.save(passportPhoto, "passport-photo", employeeId);

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
            if (i == 0) {
                doc.setIdProofPath(idProofPath);
                doc.setPassportPhotoPath(passportPhotoPath);
            }
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

        personalDetailsRepository.save(pd);
        leaveAllocationService.allocateForNewEmployee(employeeId);
        notifyHr(employee.getName(), employeeId);
    }

    // ─── HR: verify / reject ───────────────────────────────────────

    @Transactional
    public void verifyPersonalDetails(String employeeId, HrVerificationRequest request) {
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

        personalDetailsRepository.save(pd);
    }

    // ─── Admin: set PF number ──────────────────────────────────────

    @Transactional
    public void updatePfNumber(String employeeId, PfUpdateRequest request) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not found for employee: " + employeeId));
        pd.setPfNumber(request.getPfNumber());
        personalDetailsRepository.save(pd);
    }

    // ─── HR/Admin: list verifications ─────────────────────────────

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

    private void guardNotPendingOrVerified(VerificationStatus status) {
        if (status == VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Your profile is already submitted and pending HR verification.");
        if (status == VerificationStatus.VERIFIED)
            throw new BadRequestException(
                    "Your profile is already verified. Contact Admin/HR for updates.");
    }

    // Kept for future use — uncomment in updateFresherDetails / updateExperiencedDetails when needed
    private void guardOnlyRejected(VerificationStatus status) {
        if (status == VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Cannot edit while your profile is pending HR verification.");
        if (status == VerificationStatus.VERIFIED)
            throw new BadRequestException(
                    "Cannot edit a verified profile. Contact Admin/HR for updates.");
    }

    // ─────────────────────────────────────────────────────────────
    // Field-fill helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Full overwrite — used by POST. Sets every field from the request,
     * including nulling spouse fields when not MARRIED.
     */
    private void fillAllCommonFields(EmployeePersonalDetails pd, FresherPersonalDetailsRequest r) {
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

    private void fillAllCommonFields(EmployeePersonalDetails pd, ExperiencedPersonalDetailsRequest r) {
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

    /**
     * Partial patch — used by PUT (fresher).
     * Only overwrites a field if the incoming value is non-null (and non-blank for strings).
     * Spouse fields follow maritalStatus if maritalStatus itself is being changed;
     * otherwise individual spouse fields are patched independently.
     */
    private void patchCommonFields(EmployeePersonalDetails pd, FresherUpdateRequest r) {
        if (r.getFirstName()     != null && !r.getFirstName().isBlank())     pd.setFirstName(r.getFirstName());
        if (r.getLastName()      != null && !r.getLastName().isBlank())      pd.setLastName(r.getLastName());
        if (r.getContactNumber() != null && !r.getContactNumber().isBlank()) pd.setContactNumber(r.getContactNumber());
        if (r.getGender()        != null)                                    pd.setGender(r.getGender());
        if (r.getDateOfBirth()   != null)                                    pd.setDateOfBirth(r.getDateOfBirth());
        if (r.getPersonalEmail() != null && !r.getPersonalEmail().isBlank()) pd.setPersonalEmail(r.getPersonalEmail());
        if (r.getPresentAddress()  != null && !r.getPresentAddress().isBlank())  pd.setPresentAddress(r.getPresentAddress());
        if (r.getPermanentAddress()!= null && !r.getPermanentAddress().isBlank()) pd.setPermanentAddress(r.getPermanentAddress());
        if (r.getBloodGroup()    != null)                                    pd.setBloodGroup(r.getBloodGroup());
        if (r.getEmergencyContactNumber() != null && !r.getEmergencyContactNumber().isBlank())
            pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
        if (r.getAadharNumber()  != null && !r.getAadharNumber().isBlank())  pd.setAadharNumber(r.getAadharNumber());
        if (r.getDesignation()   != null && !r.getDesignation().isBlank())   pd.setDesignation(r.getDesignation());
        if (r.getSkillSet()      != null)                                    pd.setSkillSet(r.getSkillSet());
        if (r.getAccountNumber() != null && !r.getAccountNumber().isBlank()) pd.setAccountNumber(r.getAccountNumber());
        if (r.getBankName()      != null && !r.getBankName().isBlank())      pd.setBankName(r.getBankName());
        if (r.getIfscCode()      != null && !r.getIfscCode().isBlank())      pd.setIfscCode(r.getIfscCode());
        if (r.getBankBranchName()!= null && !r.getBankBranchName().isBlank()) pd.setBankBranchName(r.getBankBranchName());
        if (r.getFatherName()    != null)  pd.setFatherName(r.getFatherName());
        if (r.getFatherDateOfBirth() != null) pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        if (r.getFatherOccupation()  != null) pd.setFatherOccupation(r.getFatherOccupation());
        if (r.getFatherAlive()   != null)  pd.setFatherAlive(r.getFatherAlive());
        if (r.getMotherName()    != null)  pd.setMotherName(r.getMotherName());
        if (r.getMotherDateOfBirth() != null) pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        if (r.getMotherOccupation()  != null) pd.setMotherOccupation(r.getMotherOccupation());
        if (r.getMotherAlive()   != null)  pd.setMotherAlive(r.getMotherAlive());

        // Marital status change: if switching to/from MARRIED, apply spouse logic
        if (r.getMaritalStatus() != null) {
            pd.setMaritalStatus(r.getMaritalStatus());
            if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
                // Apply any spouse fields that were also sent
                if (r.getSpouseName()          != null) pd.setSpouseName(r.getSpouseName());
                if (r.getSpouseDateOfBirth()   != null) pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
                if (r.getSpouseOccupation()    != null) pd.setSpouseOccupation(r.getSpouseOccupation());
                if (r.getSpouseContactNumber() != null) pd.setSpouseContactNumber(r.getSpouseContactNumber());
            } else {
                // Switched away from MARRIED — clear spouse fields
                pd.setSpouseName(null);
                pd.setSpouseDateOfBirth(null);
                pd.setSpouseOccupation(null);
                pd.setSpouseContactNumber(null);
            }
        } else {
            // maritalStatus not changed — patch individual spouse fields if sent
            if (r.getSpouseName()          != null) pd.setSpouseName(r.getSpouseName());
            if (r.getSpouseDateOfBirth()   != null) pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            if (r.getSpouseOccupation()    != null) pd.setSpouseOccupation(r.getSpouseOccupation());
            if (r.getSpouseContactNumber() != null) pd.setSpouseContactNumber(r.getSpouseContactNumber());
        }
    }

    /**
     * Partial patch — used by PUT (experienced).
     * Same null-check logic as the fresher variant.
     */
    private void patchCommonFields(EmployeePersonalDetails pd, ExperiencedUpdateRequest r) {
        if (r.getFirstName()     != null && !r.getFirstName().isBlank())     pd.setFirstName(r.getFirstName());
        if (r.getLastName()      != null && !r.getLastName().isBlank())      pd.setLastName(r.getLastName());
        if (r.getContactNumber() != null && !r.getContactNumber().isBlank()) pd.setContactNumber(r.getContactNumber());
        if (r.getGender()        != null)                                    pd.setGender(r.getGender());
        if (r.getDateOfBirth()   != null)                                    pd.setDateOfBirth(r.getDateOfBirth());
        if (r.getPersonalEmail() != null && !r.getPersonalEmail().isBlank()) pd.setPersonalEmail(r.getPersonalEmail());
        if (r.getPresentAddress()  != null && !r.getPresentAddress().isBlank())  pd.setPresentAddress(r.getPresentAddress());
        if (r.getPermanentAddress()!= null && !r.getPermanentAddress().isBlank()) pd.setPermanentAddress(r.getPermanentAddress());
        if (r.getBloodGroup()    != null)                                    pd.setBloodGroup(r.getBloodGroup());
        if (r.getEmergencyContactNumber() != null && !r.getEmergencyContactNumber().isBlank())
            pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
        if (r.getAadharNumber()  != null && !r.getAadharNumber().isBlank())  pd.setAadharNumber(r.getAadharNumber());
        if (r.getDesignation()   != null && !r.getDesignation().isBlank())   pd.setDesignation(r.getDesignation());
        if (r.getSkillSet()      != null)                                    pd.setSkillSet(r.getSkillSet());
        if (r.getAccountNumber() != null && !r.getAccountNumber().isBlank()) pd.setAccountNumber(r.getAccountNumber());
        if (r.getBankName()      != null && !r.getBankName().isBlank())      pd.setBankName(r.getBankName());
        if (r.getIfscCode()      != null && !r.getIfscCode().isBlank())      pd.setIfscCode(r.getIfscCode());
        if (r.getBankBranchName()!= null && !r.getBankBranchName().isBlank()) pd.setBankBranchName(r.getBankBranchName());
        if (r.getFatherName()    != null)  pd.setFatherName(r.getFatherName());
        if (r.getFatherDateOfBirth() != null) pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        if (r.getFatherOccupation()  != null) pd.setFatherOccupation(r.getFatherOccupation());
        if (r.getFatherAlive()   != null)  pd.setFatherAlive(r.getFatherAlive());
        if (r.getMotherName()    != null)  pd.setMotherName(r.getMotherName());
        if (r.getMotherDateOfBirth() != null) pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        if (r.getMotherOccupation()  != null) pd.setMotherOccupation(r.getMotherOccupation());
        if (r.getMotherAlive()   != null)  pd.setMotherAlive(r.getMotherAlive());

        if (r.getMaritalStatus() != null) {
            pd.setMaritalStatus(r.getMaritalStatus());
            if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
                if (r.getSpouseName()          != null) pd.setSpouseName(r.getSpouseName());
                if (r.getSpouseDateOfBirth()   != null) pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
                if (r.getSpouseOccupation()    != null) pd.setSpouseOccupation(r.getSpouseOccupation());
                if (r.getSpouseContactNumber() != null) pd.setSpouseContactNumber(r.getSpouseContactNumber());
            } else {
                pd.setSpouseName(null);
                pd.setSpouseDateOfBirth(null);
                pd.setSpouseOccupation(null);
                pd.setSpouseContactNumber(null);
            }
        } else {
            if (r.getSpouseName()          != null) pd.setSpouseName(r.getSpouseName());
            if (r.getSpouseDateOfBirth()   != null) pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            if (r.getSpouseOccupation()    != null) pd.setSpouseOccupation(r.getSpouseOccupation());
            if (r.getSpouseContactNumber() != null) pd.setSpouseContactNumber(r.getSpouseContactNumber());
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
    // Profile builder helpers
    // ─────────────────────────────────────────────────────────────

    private ProfileResponse buildBaseProfile(Employee employee, User user) {
        ProfileResponse r = new ProfileResponse();
        r.setId(employee.getEmpId());
        r.setName(employee.getName());
        r.setEmail(employee.getEmail());
        r.setRole(employee.getRole().getRoleName());
        r.setReportingId(employee.getReportingId());
        r.setEmployeeExperience(employee.getEmployeeExperience());
        r.setActive(user.getStatus() == EmployeeStatus.ACTIVE);
        r.setMustChangePassword(user.isForcePwdChange());
        r.setJoiningDate(employee.getOnboarding().getJoiningDate());
        r.setBiometricStatus(employee.getOnboarding().getBiometricStatus().name());
        r.setVpnStatus(employee.getOnboarding().getVpnStatus().name());
        r.setCreatedAt(employee.getCreatedAt());
        r.setUpdatedAt(employee.getUpdatedAt());
        r.setBranch(employee.getBranch().getName());
        r.setCompanyName(employee.getBranch().getCompany().getName());
        r.setCountry(employee.getBranch().getCompany().getCountry().getName());
        if (employee.getReportingId() != null) {
            employeeRepository.findByEmpId(employee.getReportingId())
                    .ifPresent(m -> r.setReportingName(m.getName()));
        }
        return r;
    }

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
            List<ExperiencedDocumentDto> docDtos = pd.getExperiencedDocuments().stream()
                    .map(this::toExperiencedDocumentDto)
                    .collect(Collectors.toList());
            r.setExperiencedDocuments(docDtos);
        }
    }

    private ExperiencedDocumentDto toExperiencedDocumentDto(ExperiencedDocument doc) {
        ExperiencedDocumentDto dto = new ExperiencedDocumentDto();
        dto.setId(doc.getId());
        dto.setCompanyName(doc.getCompanyName());
        dto.setRole(doc.getRole());
        dto.setFromDate(doc.getFromDate());
        dto.setEndDate(doc.getEndDate());
        dto.setExperienceCertPath(doc.getExperienceCertPath());
        dto.setRelievingLetterPath(doc.getRelievingLetterPath());
        dto.setIdProofPath(doc.getIdProofPath());
        dto.setPassportPhotoPath(doc.getPassportPhotoPath());
        return dto;
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

    /** Full validation — used by POST only where all spouse fields are mandatory when MARRIED. */
    private void validateSpouseForFullSubmit(MaritalStatus status, String spouseName,
                                             LocalDate spouseDateOfBirth, String spouseOccupation,
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

    /** Returns true only if a real file was actually uploaded. */
    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
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