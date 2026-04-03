package com.emp_management.feature.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Represents one experience entry in the experienced employee form.
 *
 * The frontend sends this as JSON inside a JSON array string.
 * Each entry has its own experience certificate file, referenced
 * by a 0-based index that matches the multipart file list sent
 * under the key "experienceCerts".
 *
 * The last company is identified by setting isLastCompany = true.
 * That entry must also have an accompanying relieving letter file,
 * referenced by "relievingLetter" multipart key.
 */
public class ExperienceEntryDto {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * true = this is the most recent (last) company.
     * Only one entry in the list should have this set to true.
     * The relieving letter file is attached for this entry only.
     */
    private boolean lastCompany;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isLastCompany() { return lastCompany; }
    public void setLastCompany(boolean lastCompany) { this.lastCompany = lastCompany; }
}