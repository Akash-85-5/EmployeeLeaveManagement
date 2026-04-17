package com.emp_management.feature.skillset.controller;

import com.emp_management.feature.skillset.dto.BadgeSummaryDTO;
import com.emp_management.feature.skillset.dto.SkillRequestDTO;
import com.emp_management.feature.skillset.dto.SkillResponseDTO;
import com.emp_management.feature.skillset.service.SkillService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/skillset")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    // ── Employee Endpoints ─────────────────────────────────────────────────

    /**
     * GET /api/skillset/me
     * Fetches all skills for the logged-in employee.
     * Used by: Myskills.tsx
     */
    @GetMapping("/me")
    public List<SkillResponseDTO> getMySkills() {
        return skillService.getMySkills();
    }

    /**
     * POST /api/skillset
     * Adds a new skill. Accepts JSON (dto) and a File (proof).
     * Used by: EditModal (Add Mode) in Myskills.tsx
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkillResponseDTO addSkill(
            @RequestPart("dto") SkillRequestDTO dto,
            @RequestPart(value = "proofFile", required = false) MultipartFile proofFile) {
        return skillService.addSkill(dto, proofFile);
    }

    /**
     * PUT /api/skillset/{id}
     * Updates an existing skill.
     * Used by: EditModal (Edit Mode) in Myskills.tsx
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkillResponseDTO updateSkill(
            @PathVariable Long id,
            @RequestPart("dto") SkillRequestDTO dto,
            @RequestPart(value = "proofFile", required = false) MultipartFile proofFile) {
        return skillService.updateSkill(id, dto, proofFile);
    }

    /**
     * DELETE /api/skillset/{id}
     * Deletes a skill and its associated file.
     * Used by: Myskills.tsx
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/skillset/me/badges
     * Fetches badge statistics (counts and ratings).
     * Used by: Badges.tsx and Progression.tsx
     */
    @GetMapping("/me/badges")
    public BadgeSummaryDTO getMyBadges() {
        return skillService.getMyBadges();
    }

    /**
     * GET /api/skillset/{id}/file
     * Serves the uploaded proof file (PDF/Image).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id) {
        return skillService.serveFile(id);
    }

    // ── Manager / HR Endpoints ─────────────────────────────────────────────

    /**
     * GET /api/skillset/team
     * Fetches skills for all direct reports of the logged-in manager.
     * Used by: ManagerTeamSkills.tsx
     */
    @GetMapping("/team")
    public List<SkillResponseDTO> getTeamSkills() {
        return skillService.getTeamSkills();
    }

    /**
     * GET /api/skillset/employee/{employeeId}
     * Fetches skills for a specific employee (for HR/Manager deep dive).
     * Used by: ManagerTeamSkills.tsx (when drilling down)
     */
    @GetMapping("/employee/{employeeId}")
    public List<SkillResponseDTO> getEmployeeSkills(@PathVariable String employeeId) {
        return skillService.getEmployeeSkills(employeeId);
    }
}