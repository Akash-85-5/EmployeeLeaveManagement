package com.emp_management.feature.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * One experience entry sent from the frontend.
 *
 * Multipart file keys (indexed to match this entry's position):
 *   experienceCerts[i]  — mandatory on POST, optional on PUT
 *   joiningLetters[i]   — optional; only sent when hasJoiningLetter = true
 *   relievingLetter     — single file, only required on POST when lastCompany = true
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

    /** Exactly one entry per submission must be marked true. */
    private boolean lastCompany;

    /**
     * Set true when a joining letter file is being sent for this entry
     * in joiningLetters[i]. Service uses this flag to know whether to
     * look for a file at that index.
     */
    private boolean hasJoiningLetter;

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

    public boolean isHasJoiningLetter() { return hasJoiningLetter; }
    public void setHasJoiningLetter(boolean hasJoiningLetter) { this.hasJoiningLetter = hasJoiningLetter; }
}