package com.emp_management.feature.leave.carryforward.component;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the carry-forward leave approval chain per applicant role.
 *
 * Matrix:
 *   EMPLOYEE    → [TEAM_LEADER, MANAGER]   (2-level)
 *   TEAM_LEADER → [MANAGER,     HR]        (2-level)
 *   MANAGER     → [HR]                     (1-level)
 *   HR          → [CEO]                    (1-level)
 *   ADMIN       → [HR]                     (1-level)
 */
@Component
public class ApprovalMatrixService {

    private static final Map<String, List<String>> CHAIN = Map.of(
            "EMPLOYEE",    List.of("TEAM_LEADER", "MANAGER"),
            "TEAM_LEADER", List.of("MANAGER", "HR"),
            "MANAGER",     List.of("HR"),
            "HR",          List.of("CEO"),
            "ADMIN",       List.of("HR")
    );

    /**
     * Returns the ordered list of approver roles for a given applicant role.
     * Index 0 = level-1 approver, index 1 = level-2 approver (if present).
     */
    public List<String> getApprovalChain(String applicantRole) {
        List<String> chain = CHAIN.get(normalise(applicantRole));
        if (chain == null) {
            throw new IllegalArgumentException(
                    "No approval chain defined for role: " + applicantRole);
        }
        return chain;
    }

    /** Total number of approval levels required for this applicant role. */
    public int getTotalLevels(String applicantRole) {
        return getApprovalChain(applicantRole).size();
    }

    /** Role required at approval level 1 (always present). */
    public String getLevel1Role(String applicantRole) {
        return getApprovalChain(applicantRole).get(0);
    }

    /**
     * Role required at approval level 2.
     * Returns null for single-level flows (MANAGER, HR, ADMIN applicants).
     */
    public String getLevel2Role(String applicantRole) {
        List<String> chain = getApprovalChain(applicantRole);
        return chain.size() > 1 ? chain.get(1) : null;
    }

    /**
     * Returns the required approver role for a given approval level (1-based).
     */
    public String getRequiredRoleForLevel(String applicantRole, int level) {
        List<String> chain = getApprovalChain(applicantRole);
        if (level < 1 || level > chain.size()) {
            throw new IllegalArgumentException(
                    "Invalid approval level " + level + " for role " + applicantRole);
        }
        return chain.get(level - 1);
    }

    private String normalise(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }
}