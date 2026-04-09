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
import com.emp_management.shared.entity.Branch;
import com.emp_management.shared.entity.Department;
import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.*;
import com.emp_management.shared.exceptions.BadRequestException;
import com.emp_management.shared.exceptions.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        NameDto name = new NameDto();
        name.setEmpId(employee.getEmpId());
        name.setEmpName(employee.getName());
        return name;
    }

    public List<Department> getDepartmentList() { return departmentRepository.findAll(); }
    public List<Role> getRoleList() { return roleRepository.findAll(); }
    public List<EmployeeListDto> getAllEmployees() { return employeeRepository.findAllActiveEmployeeBasicDetails(); }
    public List<BranchListDto> getAllBranches() { return branchRepository.findAllBranchDetails(); }

    // ─── Profile read ──────────────────────────────────────────────

    public ProfileResponse getProfile(String employeeId) {
        User user = userRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        ProfileResponse response = buildBaseProfile(employee, user);

        Optional<EmployeePersonalDetails> personalOpt =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);

        if (personalOpt.isPresent()) {
            EmployeePersonalDetails pd = personalOpt.get();
            mapPersonalDetailsToResponse(pd, response);
            response.setPersonalDetailsComplete(true);
            response.setPersonalDetailsLocked(pd.isLocked());
            response.setVerificationStatus(pd.getVerificationStatus());
            if (pd.getVerificationStatus() == VerificationStatus.REJECTED)
                response.setHrRemarks(pd.getHrRemarks());
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProfileResponse response = buildBaseProfile(pd.getEmployee(), user);
        mapPersonalDetailsToResponse(pd, response);
        response.setPersonalDetailsComplete(true);
        response.setPersonalDetailsLocked(pd.isLocked());
        response.setVerificationStatus(pd.getVerificationStatus());
        if (pd.getVerificationStatus() == VerificationStatus.REJECTED)
            response.setHrRemarks(pd.getHrRemarks());
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // FRESHER — POST
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
    // EXPERIENCED — POST
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void submitExperiencedDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts,
            List<MultipartFile> joiningLetters,
            MultipartFile relievingLetter) {

        Optional<EmployeePersonalDetails> existing =
                personalDetailsRepository.findByEmployee_EmpId(employeeId);
        existing.ifPresent(pd -> guardNotPendingOrVerified(pd.getVerificationStatus()));

        saveExperiencedDetails(employeeId, dataJson,
                idProof, passportPhoto, experienceCerts, joiningLetters, relievingLetter, existing);
    }

    // ─────────────────────────────────────────────────────────────
    // UNIFIED PROFILE PUT — patches Employee + PersonalDetails
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void updateProfile(
            String employeeId,
            String dataJson,
            MultipartFile idProof,
            MultipartFile passportPhoto,
            MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet,
            MultipartFile degreeCertificate,
            MultipartFile offerLetter,
            List<MultipartFile> experienceCerts,
            List<MultipartFile> joiningLetters,
            MultipartFile relievingLetter) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "No personal details found to update for employee: " + employeeId));

        // guardOnlyRejected(pd.getVerificationStatus()); // Uncomment when HR-rejection gate needed

        ProfileUpdateRequest request = parseJson(dataJson, ProfileUpdateRequest.class);

        // ── 1. Patch Employee table fields ────────────────────────
        if (request.getName() != null && !request.getName().isBlank())
            employee.setName(request.getName());
        if(request.getJoiningDate() != null){
            EmployeeOnboarding EO = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                    .orElseThrow(()-> new EntityNotFoundException("on boarding not found"));
            EO.setJoiningDate(request.getJoiningDate());
            employeeOnboardingRepository.save(EO);
            employee.setOnboarding(EO);
        }

//        if (request.getReportingId() != null)
//            employee.setReportingId(request.getReportingId().isBlank()
//                    ? null : request.getReportingId());
//
//        if (request.getDepartmentId() != null) {
//            Department dept = departmentRepository.findById(request.getDepartmentId())
//                    .orElseThrow(() -> new BadRequestException(
//                            "Department not found: " + request.getDepartmentId()));
//            employee.setDepartment(dept);
//        }
//
//        if (request.getRoleId() != null) {
//            Role role = roleRepository.findById(request.getRoleId())
//                    .orElseThrow(() -> new BadRequestException(
//                            "Role not found: " + request.getRoleId()));
//            employee.setRole(role);
//        }
//
//        if (request.getBranchId() != null) {
//            Branch branch = branchRepository.findById(request.getBranchId())
//                    .orElseThrow(() -> new BadRequestException(
//                            "Branch not found: " + request.getBranchId()));
//            employee.setBranch(branch);
//        }

<<<<<<< HEAD
        // ── Experience entries ─────────────────────────────────────────────────
        // Rule: "experiences" absent in JSON → entries untouched (only idProof/
        //        passportPhoto on first entry can still be swapped).
        //       "experiences" present → patch text fields and optionally replace
        //        files. NO file is mandatory. Existing paths are kept when the
        //        corresponding file part is not sent.
        List<ExperienceEntryDto> experiences = request.getExperiences();
        if (experiences != null) {

            if (experiences.isEmpty())
                throw new BadRequestException("Experience entries cannot be empty. Send at least one entry.");

            // Only validate date ranges when both dates are explicitly provided
            for (int i = 0; i < experiences.size(); i++) {
                ExperienceEntryDto e = experiences.get(i);
                if (e.getFromDate() != null && e.getEndDate() != null
                        && !e.getFromDate().isBefore(e.getEndDate()))
                    throw new BadRequestException(
                            "Experience entry " + (i + 1) + ": fromDate must be before endDate.");
            }

            // Only validate lastCompany count if more than one entry marks itself as last
            long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
            if (lastCount > 1)
                throw new BadRequestException("Only one experience entry can be marked as the last company.");

            List<ExperiencedDocument> existingDocs = pd.getExperiencedDocuments();
            int oldSize = existingDocs.size();

            // Save shared single-upload files only if new ones were sent
            String newIdProofPath       = hasFile(idProof)
                    ? documentStorageService.save(idProof,       "id-proof",       employeeId) : null;
            String newPassportPhotoPath = hasFile(passportPhoto)
                    ? documentStorageService.save(passportPhoto, "passport-photo", employeeId) : null;

            if (experiences.size() == oldSize) {
                // ── Same count: patch each existing row in place ──────────────
                for (int i = 0; i < experiences.size(); i++) {
                    ExperienceEntryDto entry = experiences.get(i);
                    ExperiencedDocument doc  = existingDocs.get(i);

                    // Text — only overwrite if a value was actually sent
                    if (entry.getCompanyName() != null && !entry.getCompanyName().isBlank())
                        doc.setCompanyName(entry.getCompanyName());
                    if (entry.getRole() != null && !entry.getRole().isBlank())
                        doc.setRole(entry.getRole());
                    if (entry.getFromDate() != null) doc.setFromDate(entry.getFromDate());
                    if (entry.getEndDate()  != null) doc.setEndDate(entry.getEndDate());

                    // Experience cert — only replace if a new file was sent at this index
                    boolean newCertSent = experienceCerts != null
                            && i < experienceCerts.size()
                            && hasFile(experienceCerts.get(i));
                    if (newCertSent) {
                        documentStorageService.delete(doc.getExperienceCertPath());
                        doc.setExperienceCertPath(
                                documentStorageService.save(experienceCerts.get(i),
                                        "experience-cert", employeeId));
                    }

                    // Relieving letter — only replace if new file sent for the last-company entry
                    if (entry.isLastCompany() && hasFile(relievingLetter)) {
                        documentStorageService.delete(doc.getRelievingLetterPath());
                        doc.setRelievingLetterPath(
                                documentStorageService.save(relievingLetter,
                                        "relieving-letter", employeeId));
                    }

                    // idProof and passportPhoto live on the first entry only
                    if (i == 0) {
                        if (newIdProofPath != null) {
                            documentStorageService.delete(doc.getIdProofPath());
                            doc.setIdProofPath(newIdProofPath);
                        }
                        if (newPassportPhotoPath != null) {
                            documentStorageService.delete(doc.getPassportPhotoPath());
                            doc.setPassportPhotoPath(newPassportPhotoPath);
                        }
                    }
                }

            } else {
                // ── Count changed: rebuild list, carry forward old paths by index ──
                // Snapshot old paths before clearing so we can reuse them
                String oldIdProofPath    = oldSize > 0 ? existingDocs.get(0).getIdProofPath()       : null;
                String oldPassportPath   = oldSize > 0 ? existingDocs.get(0).getPassportPhotoPath() : null;
                List<String> oldCertPaths      = new ArrayList<>();
                List<String> oldRelievingPaths = new ArrayList<>();
                List<LocalDate> oldFromDates   = new ArrayList<>();
                List<LocalDate> oldEndDates    = new ArrayList<>();
                List<String> oldCompanyNames   = new ArrayList<>();
                List<String> oldRoles          = new ArrayList<>();
                for (ExperiencedDocument d : existingDocs) {
                    oldCertPaths.add(d.getExperienceCertPath());
                    oldRelievingPaths.add(d.getRelievingLetterPath());
                    oldFromDates.add(d.getFromDate());
                    oldEndDates.add(d.getEndDate());
                    oldCompanyNames.add(d.getCompanyName());
                    oldRoles.add(d.getRole());
                }

                // Delete disk files for entries that are being dropped (positions >= new size)
                for (int i = experiences.size(); i < oldSize; i++) {
                    documentStorageService.delete(oldCertPaths.get(i));
                    documentStorageService.delete(oldRelievingPaths.get(i));
                    if (i == 0) {
                        documentStorageService.delete(oldIdProofPath);
                        documentStorageService.delete(oldPassportPath);
                    }
                }

                pd.getExperiencedDocuments().clear();

                for (int i = 0; i < experiences.size(); i++) {
                    ExperienceEntryDto entry = experiences.get(i);
                    ExperiencedDocument doc  = new ExperiencedDocument();
                    doc.setPersonalDetails(pd);

                    // Text: use sent value, else carry old value if the position existed before
                    doc.setCompanyName(entry.getCompanyName() != null && !entry.getCompanyName().isBlank()
                            ? entry.getCompanyName() : (i < oldSize ? oldCompanyNames.get(i) : null));
                    doc.setRole(entry.getRole() != null && !entry.getRole().isBlank()
                            ? entry.getRole() : (i < oldSize ? oldRoles.get(i) : null));
                    doc.setFromDate(entry.getFromDate() != null
                            ? entry.getFromDate() : (i < oldSize ? oldFromDates.get(i) : null));
                    doc.setEndDate(entry.getEndDate() != null
                            ? entry.getEndDate() : (i < oldSize ? oldEndDates.get(i) : null));

                    // Cert: use new file if sent, else carry old path
                    boolean newCertSent = experienceCerts != null
                            && i < experienceCerts.size()
                            && hasFile(experienceCerts.get(i));
                    doc.setExperienceCertPath(newCertSent
                            ? documentStorageService.save(experienceCerts.get(i), "experience-cert", employeeId)
                            : (i < oldSize ? oldCertPaths.get(i) : null));

                    // Relieving: use new file if sent for last-company entry, else carry old
                    if (entry.isLastCompany() && hasFile(relievingLetter)) {
                        doc.setRelievingLetterPath(
                                documentStorageService.save(relievingLetter, "relieving-letter", employeeId));
                    } else {
                        doc.setRelievingLetterPath(i < oldSize ? oldRelievingPaths.get(i) : null);
                    }

                    // idProof and passportPhoto on first entry only
                    if (i == 0) {
                        doc.setIdProofPath(newIdProofPath != null ? newIdProofPath : oldIdProofPath);
                        doc.setPassportPhotoPath(newPassportPhotoPath != null ? newPassportPhotoPath : oldPassportPath);
                    }

                    pd.getExperiencedDocuments().add(doc);
                }
            }

        } else {
            // "experiences" not in JSON — only patch idProof/passportPhoto on first entry if new files sent
            if (!pd.getExperiencedDocuments().isEmpty()) {
                ExperiencedDocument firstDoc = pd.getExperiencedDocuments().get(0);
                if (hasFile(idProof)) {
                    documentStorageService.delete(firstDoc.getIdProofPath());
                    firstDoc.setIdProofPath(documentStorageService.save(idProof, "id-proof", employeeId));
                }
                if (hasFile(passportPhoto)) {
                    documentStorageService.delete(firstDoc.getPassportPhotoPath());
                    firstDoc.setPassportPhotoPath(documentStorageService.save(passportPhoto, "passport-photo", employeeId));
                }
            }
=======
        employeeRepository.save(employee);

        // ── 2. Patch PersonalDetails text fields ──────────────────
        patchPersonalDetails(pd, request);

        // ── 3. Date validation (only for fields that were sent) ───
        validateDatesForProfileUpdate(request);

        // ── 4. Patch document files ───────────────────────────────
        EmployeeExperience exp = employee.getEmployeeExperience();

        if (exp == EmployeeExperience.FRESHER) {
            patchFresherDocFiles(pd, employeeId,
                    idProof, tenthMarksheet, twelfthMarksheet,
                    degreeCertificate, offerLetter, passportPhoto);
        } else if (exp == EmployeeExperience.EXPERIENCED) {
            patchExperiencedEntries(pd, employeeId, request.getExperiences(),
                    idProof, passportPhoto, experienceCerts, joiningLetters, relievingLetter);
>>>>>>> 523ac0f4adb5a79fa972112d40b09821ea72603b
        }

        personalDetailsRepository.save(pd);
    }

    // ─────────────────────────────────────────────────────────────
    // Shared full-save logic (POST only)
    // ─────────────────────────────────────────────────────────────

    private void saveFresherDetails(
            String employeeId, String dataJson,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

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
        validateDatesForFresherSubmit(request);

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
            List<MultipartFile> experienceCerts,
            List<MultipartFile> joiningLetters,
            MultipartFile relievingLetter,
            Optional<EmployeePersonalDetails> existing) {

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

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
                    "experienceCerts count (" + experienceCerts.size()
                            + ") must match experience entries (" + experiences.size() + ").");

        for (int i = 0; i < experienceCerts.size(); i++)
            validateFile(experienceCerts.get(i), "Experience certificate for entry " + (i + 1));

        long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
        if (lastCount != 1)
            throw new BadRequestException("Exactly one experience entry must be marked as the last company.");

        for (int i = 0; i < experiences.size(); i++)
            validateExperienceDates(experiences.get(i).getFromDate(),
                    experiences.get(i).getEndDate(), i);

        validateSpouseForFullSubmit(request.getMaritalStatus(),
                request.getSpouseName(), request.getSpouseDateOfBirth(),
                request.getSpouseOccupation(), request.getSpouseContactNumber());
        validateDatesForExperiencedSubmit(request);

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

            // Joining letter — optional per entry
            if (entry.isHasJoiningLetter() && joiningLetters != null
                    && i < joiningLetters.size() && hasFile(joiningLetters.get(i))) {
                doc.setJoiningLetterPath(
                        documentStorageService.save(joiningLetters.get(i), "joining-letter", employeeId));
            }

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


    private void patchPersonalDetails(EmployeePersonalDetails pd, ProfileUpdateRequest r) {
        if (r.getFirstName()     != null && !r.getFirstName().isBlank())     pd.setFirstName(r.getFirstName());
        if (r.getLastName()      != null && !r.getLastName().isBlank())      pd.setLastName(r.getLastName());
        if (r.getContactNumber() != null && !r.getContactNumber().isBlank()) pd.setContactNumber(r.getContactNumber());
        if (r.getGender()        != null)  pd.setGender(r.getGender());
        if (r.getDateOfBirth()   != null)  pd.setDateOfBirth(r.getDateOfBirth());
        if (r.getPersonalEmail() != null && !r.getPersonalEmail().isBlank()) pd.setPersonalEmail(r.getPersonalEmail());
        if (r.getPresentAddress()   != null && !r.getPresentAddress().isBlank())   pd.setPresentAddress(r.getPresentAddress());
        if (r.getPermanentAddress() != null && !r.getPermanentAddress().isBlank()) pd.setPermanentAddress(r.getPermanentAddress());
        if (r.getBloodGroup()    != null)  pd.setBloodGroup(r.getBloodGroup());
        if (r.getEmergencyContactNumber() != null && !r.getEmergencyContactNumber().isBlank())
            pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
        if (r.getAadharNumber()  != null && !r.getAadharNumber().isBlank())  pd.setAadharNumber(r.getAadharNumber());
        if (r.getDesignation()   != null && !r.getDesignation().isBlank())   pd.setDesignation(r.getDesignation());
        if (r.getSkillSet()      != null)  pd.setSkillSet(r.getSkillSet());
        if (r.getAccountNumber() != null && !r.getAccountNumber().isBlank()) pd.setAccountNumber(r.getAccountNumber());
        if (r.getBankName()      != null && !r.getBankName().isBlank())      pd.setBankName(r.getBankName());
        if (r.getIfscCode()      != null && !r.getIfscCode().isBlank())      pd.setIfscCode(r.getIfscCode());
        if (r.getBankBranchName()!= null && !r.getBankBranchName().isBlank()) pd.setBankBranchName(r.getBankBranchName());
        if (r.getUanNumber()     != null && !r.getUanNumber().isBlank())     pd.setUanNumber(r.getUanNumber());
        if (r.getFatherName()    != null)  pd.setFatherName(r.getFatherName());
        if (r.getFatherDateOfBirth() != null) pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        if (r.getFatherOccupation()  != null) pd.setFatherOccupation(r.getFatherOccupation());
        if (r.getFatherAlive()   != null)  pd.setFatherAlive(r.getFatherAlive());
        if (r.getMotherName()    != null)  pd.setMotherName(r.getMotherName());
        if (r.getMotherDateOfBirth() != null) pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        if (r.getMotherOccupation()  != null) pd.setMotherOccupation(r.getMotherOccupation());
        if (r.getMotherAlive()   != null)  pd.setMotherAlive(r.getMotherAlive());

        // Marital status + spouse fields
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

        // Children — only replace if explicitly sent
        if (r.getChildren() != null) replaceChildren(pd, r.getChildren());
    }

    /** Patches fresher document files — only replaces files that are actually sent. */
    private void patchFresherDocFiles(
            EmployeePersonalDetails pd, String employeeId,
            MultipartFile idProof, MultipartFile tenthMarksheet,
            MultipartFile twelfthMarksheet, MultipartFile degreeCertificate,
            MultipartFile offerLetter, MultipartFile passportPhoto) {

        FresherDocument doc = Optional.ofNullable(pd.getFresherDocument())
                .orElse(new FresherDocument());

        if (hasFile(idProof))           doc.setIdProofPath(documentStorageService.save(idProof,           "id-proof",           employeeId));
        if (hasFile(tenthMarksheet))    doc.setTenthMarksheetPath(documentStorageService.save(tenthMarksheet,    "10th-marksheet",     employeeId));
        if (hasFile(twelfthMarksheet))  doc.setTwelfthMarksheetPath(documentStorageService.save(twelfthMarksheet,  "12th-marksheet",     employeeId));
        if (hasFile(degreeCertificate)) doc.setDegreeCertificatePath(documentStorageService.save(degreeCertificate, "degree-certificate", employeeId));
        if (hasFile(offerLetter))       doc.setOfferLetterPath(documentStorageService.save(offerLetter,       "offer-letter",       employeeId));
        if (hasFile(passportPhoto))     doc.setPassportPhotoPath(documentStorageService.save(passportPhoto,     "passport-photo",     employeeId));

        doc.setPersonalDetails(pd);
        pd.setFresherDocument(doc);
    }

    /**
     * Patches experienced document entries.
     * experiences = null → only patch idProof/passportPhoto on first entry if sent.
     * experiences = non-null → patch or rebuild entries, carrying forward existing paths.
     */
    private void patchExperiencedEntries(
            EmployeePersonalDetails pd, String employeeId,
            List<ExperienceEntryDto> experiences,
            MultipartFile idProof, MultipartFile passportPhoto,
            List<MultipartFile> experienceCerts,
            List<MultipartFile> joiningLetters,
            MultipartFile relievingLetter) {

        if (experiences == null) {
            // No experience changes — only swap shared files on first entry if sent
            if (!pd.getExperiencedDocuments().isEmpty()) {
                ExperiencedDocument first = pd.getExperiencedDocuments().get(0);
                if (hasFile(idProof)) {
                    documentStorageService.delete(first.getIdProofPath());
                    first.setIdProofPath(documentStorageService.save(idProof, "id-proof", employeeId));
                }
                if (hasFile(passportPhoto)) {
                    documentStorageService.delete(first.getPassportPhotoPath());
                    first.setPassportPhotoPath(documentStorageService.save(passportPhoto, "passport-photo", employeeId));
                }
            }
            return;
        }

        if (experiences.isEmpty())
            throw new BadRequestException("Experience entries cannot be empty. Send at least one entry.");

        for (int i = 0; i < experiences.size(); i++)
            validateExperienceDates(experiences.get(i).getFromDate(), experiences.get(i).getEndDate(), i);

        long lastCount = experiences.stream().filter(ExperienceEntryDto::isLastCompany).count();
        if (lastCount > 1)
            throw new BadRequestException("Only one experience entry can be marked as the last company.");

        List<ExperiencedDocument> existingDocs = pd.getExperiencedDocuments();
        int oldSize = existingDocs.size();

        // Save shared files if new ones sent
        String newIdProofPath       = hasFile(idProof)       ? documentStorageService.save(idProof,       "id-proof",       employeeId) : null;
        String newPassportPhotoPath = hasFile(passportPhoto) ? documentStorageService.save(passportPhoto, "passport-photo", employeeId) : null;

        if (experiences.size() == oldSize) {
            // ── Same count: patch in place ────────────────────────
            for (int i = 0; i < experiences.size(); i++) {
                ExperienceEntryDto entry = experiences.get(i);
                ExperiencedDocument doc  = existingDocs.get(i);

                if (entry.getCompanyName() != null && !entry.getCompanyName().isBlank()) doc.setCompanyName(entry.getCompanyName());
                if (entry.getRole()        != null && !entry.getRole().isBlank())        doc.setRole(entry.getRole());
                if (entry.getFromDate()    != null) doc.setFromDate(entry.getFromDate());
                if (entry.getEndDate()     != null) doc.setEndDate(entry.getEndDate());

                // Experience cert
                boolean newCertSent = experienceCerts != null && i < experienceCerts.size() && hasFile(experienceCerts.get(i));
                if (newCertSent) {
                    documentStorageService.delete(doc.getExperienceCertPath());
                    doc.setExperienceCertPath(documentStorageService.save(experienceCerts.get(i), "experience-cert", employeeId));
                }

                // Joining letter
                boolean newJoiningLetterSent = entry.isHasJoiningLetter()
                        && joiningLetters != null && i < joiningLetters.size()
                        && hasFile(joiningLetters.get(i));
                if (newJoiningLetterSent) {
                    documentStorageService.delete(doc.getJoiningLetterPath());
                    doc.setJoiningLetterPath(documentStorageService.save(joiningLetters.get(i), "joining-letter", employeeId));
                }

                // Relieving letter
                if (entry.isLastCompany() && hasFile(relievingLetter)) {
                    documentStorageService.delete(doc.getRelievingLetterPath());
                    doc.setRelievingLetterPath(documentStorageService.save(relievingLetter, "relieving-letter", employeeId));
                }

                // Shared files on first entry
                if (i == 0) {
                    if (newIdProofPath != null) { documentStorageService.delete(doc.getIdProofPath()); doc.setIdProofPath(newIdProofPath); }
                    if (newPassportPhotoPath != null) { documentStorageService.delete(doc.getPassportPhotoPath()); doc.setPassportPhotoPath(newPassportPhotoPath); }
                }
            }

        } else {
            // ── Count changed: snapshot old, rebuild ──────────────
            String oldIdProofPath  = oldSize > 0 ? existingDocs.get(0).getIdProofPath()       : null;
            String oldPassportPath = oldSize > 0 ? existingDocs.get(0).getPassportPhotoPath() : null;

            List<String>    oldCertPaths      = new ArrayList<>();
            List<String>    oldJoiningPaths   = new ArrayList<>();
            List<String>    oldRelievingPaths = new ArrayList<>();
            List<LocalDate> oldFromDates      = new ArrayList<>();
            List<LocalDate> oldEndDates       = new ArrayList<>();
            List<String>    oldCompanyNames   = new ArrayList<>();
            List<String>    oldRoles          = new ArrayList<>();

            for (ExperiencedDocument d : existingDocs) {
                oldCertPaths.add(d.getExperienceCertPath());
                oldJoiningPaths.add(d.getJoiningLetterPath());
                oldRelievingPaths.add(d.getRelievingLetterPath());
                oldFromDates.add(d.getFromDate());
                oldEndDates.add(d.getEndDate());
                oldCompanyNames.add(d.getCompanyName());
                oldRoles.add(d.getRole());
            }

            // Delete files for dropped entries
            for (int i = experiences.size(); i < oldSize; i++) {
                documentStorageService.delete(oldCertPaths.get(i));
                documentStorageService.delete(oldJoiningPaths.get(i));
                documentStorageService.delete(oldRelievingPaths.get(i));
                if (i == 0) {
                    documentStorageService.delete(oldIdProofPath);
                    documentStorageService.delete(oldPassportPath);
                }
            }

            pd.getExperiencedDocuments().clear();

            for (int i = 0; i < experiences.size(); i++) {
                ExperienceEntryDto entry = experiences.get(i);
                ExperiencedDocument doc  = new ExperiencedDocument();
                doc.setPersonalDetails(pd);

                doc.setCompanyName(entry.getCompanyName() != null && !entry.getCompanyName().isBlank()
                        ? entry.getCompanyName() : (i < oldSize ? oldCompanyNames.get(i) : null));
                doc.setRole(entry.getRole() != null && !entry.getRole().isBlank()
                        ? entry.getRole() : (i < oldSize ? oldRoles.get(i) : null));
                doc.setFromDate(entry.getFromDate() != null ? entry.getFromDate() : (i < oldSize ? oldFromDates.get(i) : null));
                doc.setEndDate(entry.getEndDate()   != null ? entry.getEndDate()  : (i < oldSize ? oldEndDates.get(i)  : null));

                boolean newCertSent = experienceCerts != null && i < experienceCerts.size() && hasFile(experienceCerts.get(i));
                doc.setExperienceCertPath(newCertSent
                        ? documentStorageService.save(experienceCerts.get(i), "experience-cert", employeeId)
                        : (i < oldSize ? oldCertPaths.get(i) : null));

                boolean newJoiningLetterSent = entry.isHasJoiningLetter()
                        && joiningLetters != null && i < joiningLetters.size()
                        && hasFile(joiningLetters.get(i));
                if (newJoiningLetterSent) {
                    doc.setJoiningLetterPath(documentStorageService.save(joiningLetters.get(i), "joining-letter", employeeId));
                } else {
                    doc.setJoiningLetterPath(i < oldSize ? oldJoiningPaths.get(i) : null);
                }

                if (entry.isLastCompany() && hasFile(relievingLetter)) {
                    doc.setRelievingLetterPath(documentStorageService.save(relievingLetter, "relieving-letter", employeeId));
                } else {
                    doc.setRelievingLetterPath(i < oldSize ? oldRelievingPaths.get(i) : null);
                }

                if (i == 0) {
                    doc.setIdProofPath(newIdProofPath != null ? newIdProofPath : oldIdProofPath);
                    doc.setPassportPhotoPath(newPassportPhotoPath != null ? newPassportPhotoPath : oldPassportPath);
                }

                pd.getExperiencedDocuments().add(doc);
            }
        }
    }

    // ─── HR: verify / reject ───────────────────────────────────────

    @Transactional
    public void verifyPersonalDetails(String employeeId, HrVerificationRequest request) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Personal details not found for employee: " + employeeId));

        if (pd.getVerificationStatus() != VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Profile is not in PENDING state. Current: " + pd.getVerificationStatus());
        if (request.getStatus() == VerificationStatus.PENDING)
            throw new BadRequestException("Cannot set status back to PENDING.");

        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

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

    // ─── Admin: PF number ─────────────────────────────────────────

    @Transactional
    public void updatePfNumber(String employeeId, PfUpdateRequest request) {
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
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
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public List<Employee> getTeamMembers(String managerId) {
        return employeeRepository.findByReportingId(managerId);
    }

    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByEmpIdContainingIgnoreCase(query);
    }

    public Long getActiveEmployeesCount() {
        return employeeRepository.countByActive(true);
    }

    public void decideVpn(String employeeId, BiometricVpnStatus decision) {
        Employee employee = employeeRepository.findByEmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (employee.getOnboarding().getBiometricStatus() == BiometricVpnStatus.PROVIDED) {
            EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Onboarding not found"));
            eo.setBiometricStatus(decision);
            employeeOnboardingRepository.save(eo);
        }
        EmployeeOnboarding eo = employeeOnboardingRepository.findByEmployee_EmpId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding not found"));
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

    // Kept for future use — uncomment in updateProfile when HR-rejection gate is needed
    private void guardOnlyRejected(VerificationStatus status) {
        if (status == VerificationStatus.PENDING)
            throw new BadRequestException(
                    "Cannot edit while your profile is pending HR verification.");
        if (status == VerificationStatus.VERIFIED)
            throw new BadRequestException(
                    "Cannot edit a verified profile. Contact Admin/HR for updates.");
    }

    // ─────────────────────────────────────────────────────────────
    // Full field-fill helpers (POST only)
    // ─────────────────────────────────────────────────────────────

    private void fillAllCommonFields(EmployeePersonalDetails pd, FresherPersonalDetailsRequest r) {
        pd.setFirstName(r.getFirstName());        pd.setLastName(r.getLastName());
        pd.setContactNumber(r.getContactNumber()); pd.setGender(r.getGender());
        pd.setMaritalStatus(r.getMaritalStatus()); pd.setAadharNumber(r.getAadharNumber());
        pd.setPersonalEmail(r.getPersonalEmail()); pd.setDateOfBirth(r.getDateOfBirth());
        pd.setPresentAddress(r.getPresentAddress()); pd.setPermanentAddress(r.getPermanentAddress());
        pd.setBloodGroup(r.getBloodGroup()); pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
        pd.setDesignation(r.getDesignation()); pd.setSkillSet(r.getSkillSet());
        pd.setAccountNumber(r.getAccountNumber()); pd.setBankName(r.getBankName());
        pd.setIfscCode(r.getIfscCode()); pd.setBankBranchName(r.getBankBranchName());
        pd.setFatherName(r.getFatherName()); pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        pd.setFatherOccupation(r.getFatherOccupation()); pd.setFatherAlive(r.getFatherAlive());
        pd.setMotherName(r.getMotherName()); pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        pd.setMotherOccupation(r.getMotherOccupation()); pd.setMotherAlive(r.getMotherAlive());
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
            pd.setSpouseName(r.getSpouseName()); pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            pd.setSpouseOccupation(r.getSpouseOccupation()); pd.setSpouseContactNumber(r.getSpouseContactNumber());
        } else {
            pd.setSpouseName(null); pd.setSpouseDateOfBirth(null);
            pd.setSpouseOccupation(null); pd.setSpouseContactNumber(null);
        }
    }

    private void fillAllCommonFields(EmployeePersonalDetails pd, ExperiencedPersonalDetailsRequest r) {
        pd.setFirstName(r.getFirstName());        pd.setLastName(r.getLastName());
        pd.setContactNumber(r.getContactNumber()); pd.setGender(r.getGender());
        pd.setMaritalStatus(r.getMaritalStatus()); pd.setAadharNumber(r.getAadharNumber());
        pd.setPersonalEmail(r.getPersonalEmail()); pd.setDateOfBirth(r.getDateOfBirth());
        pd.setPresentAddress(r.getPresentAddress()); pd.setPermanentAddress(r.getPermanentAddress());
        pd.setBloodGroup(r.getBloodGroup()); pd.setEmergencyContactNumber(r.getEmergencyContactNumber());
        pd.setDesignation(r.getDesignation()); pd.setSkillSet(r.getSkillSet());
        pd.setAccountNumber(r.getAccountNumber()); pd.setBankName(r.getBankName());
        pd.setIfscCode(r.getIfscCode()); pd.setBankBranchName(r.getBankBranchName());
        pd.setFatherName(r.getFatherName()); pd.setFatherDateOfBirth(r.getFatherDateOfBirth());
        pd.setFatherOccupation(r.getFatherOccupation()); pd.setFatherAlive(r.getFatherAlive());
        pd.setMotherName(r.getMotherName()); pd.setMotherDateOfBirth(r.getMotherDateOfBirth());
        pd.setMotherOccupation(r.getMotherOccupation()); pd.setMotherAlive(r.getMotherAlive());
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) {
            pd.setSpouseName(r.getSpouseName()); pd.setSpouseDateOfBirth(r.getSpouseDateOfBirth());
            pd.setSpouseOccupation(r.getSpouseOccupation()); pd.setSpouseContactNumber(r.getSpouseContactNumber());
        } else {
            pd.setSpouseName(null); pd.setSpouseDateOfBirth(null);
            pd.setSpouseOccupation(null); pd.setSpouseContactNumber(null);
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
            documentStorageService.delete(doc.getJoiningLetterPath());
            documentStorageService.delete(doc.getRelievingLetterPath());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Profile response mapping
    // ─────────────────────────────────────────────────────────────

    private ProfileResponse buildBaseProfile(Employee employee, User user) {
        ProfileResponse r = new ProfileResponse();
        r.setId(employee.getEmpId());
        r.setName(employee.getName());
        r.setEmail(employee.getEmail());
        r.setRole(employee.getRole().getRoleName());
        r.setDepartmentName(employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null);
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
        if (employee.getReportingId() != null)
            employeeRepository.findByEmpId(employee.getReportingId())
                    .ifPresent(m -> r.setReportingName(m.getName()));
        return r;
    }

    private void mapPersonalDetailsToResponse(EmployeePersonalDetails pd, ProfileResponse r) {
        r.setFirstName(pd.getFirstName());             r.setLastName(pd.getLastName());
        r.setContactNumber(pd.getContactNumber());     r.setGender(pd.getGender());
        r.setMaritalStatus(pd.getMaritalStatus());     r.setAadharNumber(pd.getAadharNumber());
        r.setPersonalEmail(pd.getPersonalEmail());     r.setDateOfBirth(pd.getDateOfBirth());
        r.setPresentAddress(pd.getPresentAddress());   r.setPermanentAddress(pd.getPermanentAddress());
        r.setBloodGroup(pd.getBloodGroup());           r.setEmergencyContactNumber(pd.getEmergencyContactNumber());
        r.setFatherName(pd.getFatherName());           r.setMotherName(pd.getMotherName());
        r.setDesignation(pd.getDesignation());
        r.setAccountNumber(pd.getAccountNumber());     r.setBankName(pd.getBankName());
        r.setIfscCode(pd.getIfscCode());               r.setBankBranchName(pd.getBankBranchName());
        r.setPfNumber(pd.getPfNumber());               r.setUanNumber(pd.getUanNumber());
        r.setSpouseName(pd.getSpouseName());           r.setSpouseDateOfBirth(pd.getSpouseDateOfBirth());
        r.setSpouseOccupation(pd.getSpouseOccupation()); r.setSpouseContactNumber(pd.getSpouseContactNumber());

        if (pd.getSkillSet() != null && !pd.getSkillSet().isBlank())
            r.setSkillSet(Arrays.stream(pd.getSkillSet().split(","))
                    .map(String::trim).collect(Collectors.toList()));

        if (pd.getChildren() != null)
            r.setChildren(pd.getChildren().stream().map(c -> {
                ChildDto dto = new ChildDto();
                dto.setChildName(c.getChildName());
                dto.setGender(c.getGender());
                dto.setAge(c.getAge());
                return dto;
            }).collect(Collectors.toList()));

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
            r.setExperiencedDocuments(pd.getExperiencedDocuments().stream()
                    .map(this::toExperiencedDocumentDto)
                    .collect(Collectors.toList()));
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
        dto.setJoiningLetterPath(doc.getJoiningLetterPath());
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
        for (Employee hr : hrEmployees)
            userRepository.findByEmployee_EmpId(hr.getEmpId()).ifPresent(hrUser ->
                    notificationService.createNotification(
                            hr.getEmpId(), hr.getEmail(), hrUser.getEmail(),
                            EventType.PROFILE_SUBMITTED, Channel.EMAIL, msg));
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

    private static final int MIN_EMPLOYEE_AGE = 18;
    private static final int MAX_CHILD_AGE    = 17;

    private void validateEmployeeDob(LocalDate dob) {
        if (dob == null) return;
        LocalDate today = LocalDate.now();
        if (!dob.isBefore(today))
            throw new BadRequestException("Date of birth must be in the past.");
        if (dob.isAfter(today.minusYears(MIN_EMPLOYEE_AGE)))
            throw new BadRequestException("Employee must be at least " + MIN_EMPLOYEE_AGE + " years old.");
    }

    private void validateParentDob(LocalDate dob, String label) {
        if (dob == null) return;
        if (!dob.isBefore(LocalDate.now()))
            throw new BadRequestException(label + " date of birth must be in the past.");
    }

    private void validateSpouseDob(LocalDate dob) {
        if (dob == null) return;
        LocalDate today = LocalDate.now();
        if (!dob.isBefore(today))
            throw new BadRequestException("Spouse date of birth must be in the past.");
        if (dob.isAfter(today.minusYears(MIN_EMPLOYEE_AGE)))
            throw new BadRequestException("Spouse must be at least " + MIN_EMPLOYEE_AGE + " years old.");
    }

    private void validateChildren(List<ChildDto> children) {
        if (children == null || children.isEmpty()) return;
        for (int i = 0; i < children.size(); i++) {
            ChildDto c = children.get(i);
            if (c.getChildName() == null || c.getChildName().isBlank())
                throw new BadRequestException("Child " + (i + 1) + " name is required.");
            if (c.getGender() == null)
                throw new BadRequestException("Child " + (i + 1) + " gender is required.");
            if (c.getAge() == null)
                throw new BadRequestException("Child " + (i + 1) + " age is required.");
            if (c.getAge() < 0 || c.getAge() > MAX_CHILD_AGE)
                throw new BadRequestException(
                        "Child " + (i + 1) + " age must be between 0 and " + MAX_CHILD_AGE + ".");
        }
    }

    private void validateExperienceDates(LocalDate fromDate, LocalDate endDate, int idx) {
        if (fromDate == null || endDate == null) return;
        if (!fromDate.isBefore(endDate))
            throw new BadRequestException("Experience entry " + (idx + 1) + ": fromDate must be before endDate.");
        if (endDate.isAfter(LocalDate.now()))
            throw new BadRequestException("Experience entry " + (idx + 1) + ": endDate cannot be in the future.");
    }

    private void validateDatesForFresherSubmit(FresherPersonalDetailsRequest r) {
        validateEmployeeDob(r.getDateOfBirth());
        validateParentDob(r.getFatherDateOfBirth(), "Father");
        validateParentDob(r.getMotherDateOfBirth(), "Mother");
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) validateSpouseDob(r.getSpouseDateOfBirth());
        validateChildren(r.getChildren());
    }

    private void validateDatesForExperiencedSubmit(ExperiencedPersonalDetailsRequest r) {
        validateEmployeeDob(r.getDateOfBirth());
        validateParentDob(r.getFatherDateOfBirth(), "Father");
        validateParentDob(r.getMotherDateOfBirth(), "Mother");
        if (r.getMaritalStatus() == MaritalStatus.MARRIED) validateSpouseDob(r.getSpouseDateOfBirth());
        validateChildren(r.getChildren());
        if (r.getExperiences() != null)
            for (int i = 0; i < r.getExperiences().size(); i++)
                validateExperienceDates(r.getExperiences().get(i).getFromDate(),
                        r.getExperiences().get(i).getEndDate(), i);
    }

    /** PUT — validates only the fields that are present in the request. */
    private void validateDatesForProfileUpdate(ProfileUpdateRequest r) {
        validateEmployeeDob(r.getDateOfBirth());
        validateParentDob(r.getFatherDateOfBirth(), "Father");
        validateParentDob(r.getMotherDateOfBirth(), "Mother");
        if (r.getSpouseDateOfBirth() != null) validateSpouseDob(r.getSpouseDateOfBirth());
        if (r.getChildren() != null) validateChildren(r.getChildren());
        // Experience entry dates are validated inside patchExperiencedEntries
    }

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

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new BadRequestException("Invalid request format. Please check your input data.");
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