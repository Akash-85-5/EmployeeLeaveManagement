package com.emp_management.feature.skillset.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.skillset.dto.BadgeSummaryDTO;
import com.emp_management.feature.skillset.dto.SkillRequestDTO;
import com.emp_management.feature.skillset.dto.SkillResponseDTO;
import com.emp_management.feature.skillset.entity.Skill;
import com.emp_management.shared.enums.SkillCategory;
import com.emp_management.feature.skillset.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;

    // Storage root — matches existing attachment pattern in your project.
    // Override in application.properties: skillset.upload.dir=./uploads/skills
    @Value("${skillset.upload.dir:./uploads/skills}")
    private String uploadDir;

    public SkillService(SkillRepository skillRepository,
                        EmployeeRepository employeeRepository) {
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Extracts the authenticated employee's email from Spring Security context,
     * then looks up the Employee by email. This matches your existing JWT filter
     * pattern where the principal is set to the employee's email.
     */
    private Employee getAuthenticatedEmployee() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated employee not found: " + email));
    }

    // ── GET /api/skillset/me ───────────────────────────────────────────────

    public List<SkillResponseDTO> getMySkills() {
        Employee emp = getAuthenticatedEmployee();
        return skillRepository
                .findByEmployee_EmpIdOrderByCreatedAtDesc(emp.getEmpId())
                .stream()
                .map(SkillResponseDTO::from)
                .collect(Collectors.toList());
    }

    // ── POST /api/skillset ────────────────────────────────────────────────

    public SkillResponseDTO addSkill(SkillRequestDTO dto, MultipartFile proofFile) {
        Employee emp = getAuthenticatedEmployee();

        Skill skill = new Skill();
        skill.setEmployee(emp);
        mapDtoToEntity(dto, skill);
        handleFileUpload(proofFile, skill);

        return SkillResponseDTO.from(skillRepository.save(skill));
    }

    // ── PUT /api/skillset/{id} ────────────────────────────────────────────

    public SkillResponseDTO updateSkill(Long id, SkillRequestDTO dto, MultipartFile proofFile) {
        Employee emp = getAuthenticatedEmployee();

        Skill skill = skillRepository.findByIdAndEmployee_EmpId(id, emp.getEmpId())
                .orElseThrow(() -> new RuntimeException("Skill not found or not owned by you: " + id));

        mapDtoToEntity(dto, skill);

        // Only replace file if a new one was uploaded
        if (proofFile != null && !proofFile.isEmpty()) {
            deleteOldFile(skill.getProofFilePath());
            handleFileUpload(proofFile, skill);
        }

        return SkillResponseDTO.from(skillRepository.save(skill));
    }

    // ── GET /api/skillset/{id} ────────────────────────────────────────────

    public SkillResponseDTO getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        return SkillResponseDTO.from(skill);
    }

    // ── DELETE /api/skillset/{id} ─────────────────────────────────────────

    public void deleteSkill(Long id) {
        Employee emp = getAuthenticatedEmployee();
        Skill skill = skillRepository.findByIdAndEmployee_EmpId(id, emp.getEmpId())
                .orElseThrow(() -> new RuntimeException("Skill not found or not owned by you: " + id));
        deleteOldFile(skill.getProofFilePath());
        skillRepository.delete(skill);
    }

    // ── GET /api/skillset/team ────────────────────────────────────────────
    // Manager sees skills of all their direct reports

    public List<SkillResponseDTO> getTeamSkills() {
        Employee manager = getAuthenticatedEmployee();
        return skillRepository.findAllByManagerId(manager.getEmpId())
                .stream()
                .map(SkillResponseDTO::from)
                .collect(Collectors.toList());
    }

    // ── GET /api/skillset/employee/{employeeId} ────────────────────────────
    // HR / manager can view any specific employee's skills

    public List<SkillResponseDTO> getEmployeeSkills(String employeeId) {
        return skillRepository.findByEmployee_EmpIdOrderBySkillNameAsc(employeeId)
                .stream()
                .map(SkillResponseDTO::from)
                .collect(Collectors.toList());
    }

    // ── GET /api/skillset/me/badges ───────────────────────────────────────

    public BadgeSummaryDTO getMyBadges() {
        Employee emp = getAuthenticatedEmployee();
        String empId = emp.getEmpId();

        int techCount  = skillRepository.countByEmpIdAndCategory(empId, SkillCategory.TECHNICAL);
        int toolsCount = skillRepository.countByEmpIdAndCategory(empId, SkillCategory.TOOLS);
        int softCount  = skillRepository.countByEmpIdAndCategory(empId, SkillCategory.INTERPERSONAL);

        Double avgTechRaw  = skillRepository.avgRatingByEmpIdAndCategory(empId, SkillCategory.TECHNICAL);
        Double avgToolsRaw = skillRepository.avgRatingByEmpIdAndCategory(empId, SkillCategory.TOOLS);
        Double avgSoftRaw  = skillRepository.avgRatingByEmpIdAndCategory(empId, SkillCategory.INTERPERSONAL);

        double avgTech  = avgTechRaw  != null ? avgTechRaw  : 0.0;
        double avgTools = avgToolsRaw != null ? avgToolsRaw : 0.0;
        double avgSoft  = avgSoftRaw  != null ? avgSoftRaw  : 0.0;

        return BadgeSummaryDTO.compute(techCount, toolsCount, softCount,
                avgTech, avgTools, avgSoft);
    }

    // ── GET /api/skillset/{id}/file ────────────────────────────────────────

    public ResponseEntity<Resource> serveFile(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        if (skill.getProofFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(skill.getProofFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + skill.getProofFileName() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not serve file: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Could not determine file type: " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void mapDtoToEntity(SkillRequestDTO dto, Skill skill) {
        if (dto.getSkillName() != null) skill.setSkillName(dto.getSkillName());
        if (dto.getCategory()  != null) skill.setCategory(dto.getCategory());
        if (dto.getRating()    != null) skill.setRating(dto.getRating());
        if (dto.getLearnedAt() != null) skill.setLearnedAt(dto.getLearnedAt());
        if (dto.getAppliedAt() != null) skill.setAppliedAt(dto.getAppliedAt());
        if (dto.getDateLearned() != null) skill.setDateLearned(dto.getDateLearned());
        if (dto.getCertDate()    != null) skill.setCertDate(dto.getCertDate());
    }

    private void handleFileUpload(MultipartFile file, Skill skill) {
        if (file == null || file.isEmpty()) return;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            String extension    = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            String storedName   = UUID.randomUUID() + extension;
            Path targetPath     = uploadPath.resolve(storedName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            skill.setProofFilePath(targetPath.toString());
            skill.setProofFileName(originalName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store proof file: " + e.getMessage(), e);
        }
    }

    private void deleteOldFile(String filePath) {
        if (filePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ignored) {
            // Non-critical — log in production
        }
    }
}