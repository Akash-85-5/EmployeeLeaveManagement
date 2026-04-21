package com.emp_management.shared.enums;

/**
 * Maps to the frontend SkillCategory type:
 *   "Technical" | "Tools" | "Platforms" | "Interpersonal"
 *
 * Frontend filter shows 4 options but "Tools" and "Platforms" are combined
 * into TOOLS in the backend. The response DTO returns "Tools & Platforms"
 * as the display label.
 */
public enum SkillCategory {
    TECHNICAL,      // frontend: "Technical"
    TOOLS,          // frontend: "Tools" or "Platforms" (combined)
    INTERPERSONAL   // frontend: "Interpersonal"

    // ── Helper to parse from frontend string ─────────────────────────────
    ;

    public static SkillCategory fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "technical" -> TECHNICAL;
            case "tools", "platforms", "tools & platforms" -> TOOLS;
            case "interpersonal" -> INTERPERSONAL;
            default -> throw new IllegalArgumentException(
                    "Unknown skill category: " + value +
                            ". Valid values: Technical, Tools, Platforms, Interpersonal");
        };
    }

    /**
     * Returns the display label matching frontend expectations
     */
    public String getDisplayLabel() {
        return switch (this) {
            case TECHNICAL -> "Technical";
            case TOOLS -> "Tools & Platforms";
            case INTERPERSONAL -> "Interpersonal";
        };
    }
}