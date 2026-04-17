package com.emp_management.feature.skillset.dto;

/**
 * Returned by GET /api/skillset/me/badges
 *
 * Powers both Badges.tsx (badge unlock logic) and Progression.tsx (pie chart,
 * stat cards, deep-dive section).
 *
 * Badge unlock rules (mirrors frontend badge definitions):
 *   Tech + Tools combined ≥  5  → Associate badge   (Tier I)
 *   Tech + Tools combined ≥ 12  → Specialist badge  (Tier II)
 *   Tech + Tools combined ≥ 20  → Authority badge   (Tier III) [gold]
 *   Interpersonal          ≥  3  → Professional Core (Tier I)
 *   Interpersonal          ≥ 10  → Collaborator      (Tier II)
 *   Interpersonal          ≥ 15  → Strategic Lead    (Tier III) [gold]
 */
public class BadgeSummaryDTO {

    // ── Raw skill counts ───────────────────────────────────────────────────
    private int technicalCount;
    private int toolsCount;
    private int interpersonalCount;
    private int totalCount;

    // ── Computed for badge logic ───────────────────────────────────────────
    private int techToolCombined;           // technicalCount + toolsCount

    // ── Average proficiency per category ──────────────────────────────────
    private double avgTechnicalRating;
    private double avgToolsRating;
    private double avgInterpersonalRating;

    // ── Badge unlock flags ─────────────────────────────────────────────────
    private boolean techAssociateEarned;    // techToolCombined >= 5
    private boolean techSpecialistEarned;   // techToolCombined >= 12
    private boolean techAuthorityEarned;    // techToolCombined >= 20

    private boolean softProfessionalEarned; // interpersonalCount >= 3
    private boolean softCollaboratorEarned; // interpersonalCount >= 10
    private boolean softStrategicEarned;    // interpersonalCount >= 15

    // ── Factory ───────────────────────────────────────────────────────────

    public static BadgeSummaryDTO compute(
            int techCount, int toolsCount, int softCount,
            double avgTech, double avgTools, double avgSoft) {

        BadgeSummaryDTO dto = new BadgeSummaryDTO();
        dto.technicalCount       = techCount;
        dto.toolsCount           = toolsCount;
        dto.interpersonalCount   = softCount;
        dto.totalCount           = techCount + toolsCount + softCount;
        dto.techToolCombined     = techCount + toolsCount;

        dto.avgTechnicalRating     = avgTech;
        dto.avgToolsRating         = avgTools;
        dto.avgInterpersonalRating = avgSoft;

        dto.techAssociateEarned  = dto.techToolCombined >= 5;
        dto.techSpecialistEarned = dto.techToolCombined >= 12;
        dto.techAuthorityEarned  = dto.techToolCombined >= 20;

        dto.softProfessionalEarned = softCount >= 3;
        dto.softCollaboratorEarned = softCount >= 10;
        dto.softStrategicEarned    = softCount >= 15;

        return dto;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getTechnicalCount() { return technicalCount; }
    public void setTechnicalCount(int technicalCount) { this.technicalCount = technicalCount; }

    public int getToolsCount() { return toolsCount; }
    public void setToolsCount(int toolsCount) { this.toolsCount = toolsCount; }

    public int getInterpersonalCount() { return interpersonalCount; }
    public void setInterpersonalCount(int interpersonalCount) { this.interpersonalCount = interpersonalCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getTechToolCombined() { return techToolCombined; }
    public void setTechToolCombined(int techToolCombined) { this.techToolCombined = techToolCombined; }

    public double getAvgTechnicalRating() { return avgTechnicalRating; }
    public void setAvgTechnicalRating(double avgTechnicalRating) { this.avgTechnicalRating = avgTechnicalRating; }

    public double getAvgToolsRating() { return avgToolsRating; }
    public void setAvgToolsRating(double avgToolsRating) { this.avgToolsRating = avgToolsRating; }

    public double getAvgInterpersonalRating() { return avgInterpersonalRating; }
    public void setAvgInterpersonalRating(double avgInterpersonalRating) { this.avgInterpersonalRating = avgInterpersonalRating; }

    public boolean isTechAssociateEarned() { return techAssociateEarned; }
    public void setTechAssociateEarned(boolean techAssociateEarned) { this.techAssociateEarned = techAssociateEarned; }

    public boolean isTechSpecialistEarned() { return techSpecialistEarned; }
    public void setTechSpecialistEarned(boolean techSpecialistEarned) { this.techSpecialistEarned = techSpecialistEarned; }

    public boolean isTechAuthorityEarned() { return techAuthorityEarned; }
    public void setTechAuthorityEarned(boolean techAuthorityEarned) { this.techAuthorityEarned = techAuthorityEarned; }

    public boolean isSoftProfessionalEarned() { return softProfessionalEarned; }
    public void setSoftProfessionalEarned(boolean softProfessionalEarned) { this.softProfessionalEarned = softProfessionalEarned; }

    public boolean isSoftCollaboratorEarned() { return softCollaboratorEarned; }
    public void setSoftCollaboratorEarned(boolean softCollaboratorEarned) { this.softCollaboratorEarned = softCollaboratorEarned; }

    public boolean isSoftStrategicEarned() { return softStrategicEarned; }
    public void setSoftStrategicEarned(boolean softStrategicEarned) { this.softStrategicEarned = softStrategicEarned; }
}