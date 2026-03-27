package com.example.employeeLeaveApplication.feature.noticePeriod.entity;

import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import com.example.employeeLeaveApplication.shared.enums.TerminationGrounds;
import com.example.employeeLeaveApplication.shared.enums.TerminationType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_separation",
        indexes = {
                @Index(name = "idx_sep_employee_id", columnList = "employee_id"),
                @Index(name = "idx_sep_status",      columnList = "status"),
                @Index(name = "idx_sep_type",        columnList = "separation_type")
        }
)
public class EmployeeSeparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Who is leaving ────────────────────────────────────────────
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "employee_role", length = 30)
    private String employeeRole;

    // ── Who submitted this form ───────────────────────────────────
    // For RESIGNATION  → same as employeeId (the employee themselves)
    // For TERMINATION  → manager or HR
    // For DEATH        → manager or HR
    // For ABSCONDING   → manager or HR
    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Column(name = "initiator_role", length = 30)
    private String initiatorRole;

    // ── What type of separation ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "separation_type", nullable = false, length = 30)
    private SeparationType separationType;

    // Only for TERMINATION
    @Enumerated(EnumType.STRING)
    @Column(name = "termination_type", length = 30)
    private TerminationType terminationType;

    // Only for TERMINATION WITHOUT_NOTICE_PERIOD
    // This is the ELIGIBILITY CONDITION (not the reason text)
    // PROBATION / INTERNSHIP / MISCONDUCT
    @Enumerated(EnumType.STRING)
    @Column(name = "termination_grounds", length = 30)
    private TerminationGrounds terminationGrounds;

    // ── Reason (always free text typed by the submitter) ─────────
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    // Only for DEATH_IN_SERVICE
    @Column(name = "death_date")
    private LocalDate deathDate;

    // ── Approval workflow status ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SeparationStatus status;

    // Manager approval
    @Column(name = "approved_by_manager")
    private Long approvedByManager;

    @Column(name = "manager_action_at")
    private LocalDateTime managerActionAt;

    @Column(name = "manager_remarks", length = 500)
    private String managerRemarks;

    // HR approval
    @Column(name = "approved_by_hr")
    private Long approvedByHr;

    @Column(name = "hr_action_at")
    private LocalDateTime hrActionAt;

    @Column(name = "hr_remarks", length = 500)
    private String hrRemarks;

    // CEO approval
    @Column(name = "approved_by_ceo")
    private Long approvedByCeo;

    @Column(name = "ceo_action_at")
    private LocalDateTime ceoActionAt;

    @Column(name = "ceo_remarks", length = 500)
    private String ceoRemarks;

    // ── Notice period ─────────────────────────────────────────────
    @Column(name = "notice_period_start")
    private LocalDate noticePeriodStart;

    // Base = start + 3 months. Extended by 1 day per LOP absence.
    @Column(name = "notice_period_end")
    private LocalDate noticePeriodEnd;

    // Tracks how many LOP days have been added so far
    @Column(name = "lop_days_in_notice", nullable = false)
    private int lopDaysInNotice = 0;

    // ── Exit checklist (Admin fills at end of notice period) ──────
    @Column(name = "laptop_returned", nullable = false)
    private boolean laptopReturned = false;

    @Column(name = "id_card_returned", nullable = false)
    private boolean idCardReturned = false;

    @Column(name = "kt_completed", nullable = false)
    private boolean ktCompleted = false;

    @Column(name = "exit_checklist_completed_by")
    private Long exitChecklistCompletedBy;

    @Column(name = "exit_checklist_completed_at")
    private LocalDateTime exitChecklistCompletedAt;

    // ── Final payslip (CFO confirms after exit checklist) ─────────
    // Note: actual payslip is generated in the CFO/payslip module.
    // This flag just marks that it has been generated.
    @Column(name = "final_payslip_generated", nullable = false)
    private boolean finalPayslipGenerated = false;

    @Column(name = "final_payslip_generated_at")
    private LocalDateTime finalPayslipGeneratedAt;

    @Column(name = "final_payslip_generated_by")
    private Long finalPayslipGeneratedBy;

    // ── Account deactivation (Admin does at the very end) ─────────
    @Column(name = "account_deactivated", nullable = false)
    private boolean accountDeactivated = false;

    @Column(name = "account_deactivated_at")
    private LocalDateTime accountDeactivatedAt;

    @Column(name = "deactivated_by")
    private Long deactivatedBy;

    // ── Timestamps ────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(String employeeRole) { this.employeeRole = employeeRole; }

    public Long getInitiatorId() { return initiatorId; }
    public void setInitiatorId(Long initiatorId) { this.initiatorId = initiatorId; }

    public String getInitiatorRole() { return initiatorRole; }
    public void setInitiatorRole(String initiatorRole) { this.initiatorRole = initiatorRole; }

    public SeparationType getSeparationType() { return separationType; }
    public void setSeparationType(SeparationType separationType) { this.separationType = separationType; }

    public TerminationType getTerminationType() { return terminationType; }
    public void setTerminationType(TerminationType terminationType) { this.terminationType = terminationType; }

    public TerminationGrounds getTerminationGrounds() { return terminationGrounds; }
    public void setTerminationGrounds(TerminationGrounds terminationGrounds) { this.terminationGrounds = terminationGrounds; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getDeathDate() { return deathDate; }
    public void setDeathDate(LocalDate deathDate) { this.deathDate = deathDate; }

    public SeparationStatus getStatus() { return status; }
    public void setStatus(SeparationStatus status) { this.status = status; }

    public Long getApprovedByManager() { return approvedByManager; }
    public void setApprovedByManager(Long approvedByManager) { this.approvedByManager = approvedByManager; }

    public LocalDateTime getManagerActionAt() { return managerActionAt; }
    public void setManagerActionAt(LocalDateTime managerActionAt) { this.managerActionAt = managerActionAt; }

    public String getManagerRemarks() { return managerRemarks; }
    public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }

    public Long getApprovedByHr() { return approvedByHr; }
    public void setApprovedByHr(Long approvedByHr) { this.approvedByHr = approvedByHr; }

    public LocalDateTime getHrActionAt() { return hrActionAt; }
    public void setHrActionAt(LocalDateTime hrActionAt) { this.hrActionAt = hrActionAt; }

    public String getHrRemarks() { return hrRemarks; }
    public void setHrRemarks(String hrRemarks) { this.hrRemarks = hrRemarks; }

    public Long getApprovedByCeo() { return approvedByCeo; }
    public void setApprovedByCeo(Long approvedByCeo) { this.approvedByCeo = approvedByCeo; }

    public LocalDateTime getCeoActionAt() { return ceoActionAt; }
    public void setCeoActionAt(LocalDateTime ceoActionAt) { this.ceoActionAt = ceoActionAt; }

    public String getCeoRemarks() { return ceoRemarks; }
    public void setCeoRemarks(String ceoRemarks) { this.ceoRemarks = ceoRemarks; }

    public LocalDate getNoticePeriodStart() { return noticePeriodStart; }
    public void setNoticePeriodStart(LocalDate noticePeriodStart) { this.noticePeriodStart = noticePeriodStart; }

    public LocalDate getNoticePeriodEnd() { return noticePeriodEnd; }
    public void setNoticePeriodEnd(LocalDate noticePeriodEnd) { this.noticePeriodEnd = noticePeriodEnd; }

    public int getLopDaysInNotice() { return lopDaysInNotice; }
    public void setLopDaysInNotice(int lopDaysInNotice) { this.lopDaysInNotice = lopDaysInNotice; }

    public boolean isLaptopReturned() { return laptopReturned; }
    public void setLaptopReturned(boolean laptopReturned) { this.laptopReturned = laptopReturned; }

    public boolean isIdCardReturned() { return idCardReturned; }
    public void setIdCardReturned(boolean idCardReturned) { this.idCardReturned = idCardReturned; }

    public boolean isKtCompleted() { return ktCompleted; }
    public void setKtCompleted(boolean ktCompleted) { this.ktCompleted = ktCompleted; }

    public Long getExitChecklistCompletedBy() { return exitChecklistCompletedBy; }
    public void setExitChecklistCompletedBy(Long exitChecklistCompletedBy) { this.exitChecklistCompletedBy = exitChecklistCompletedBy; }

    public LocalDateTime getExitChecklistCompletedAt() { return exitChecklistCompletedAt; }
    public void setExitChecklistCompletedAt(LocalDateTime exitChecklistCompletedAt) { this.exitChecklistCompletedAt = exitChecklistCompletedAt; }

    public boolean isFinalPayslipGenerated() { return finalPayslipGenerated; }
    public void setFinalPayslipGenerated(boolean finalPayslipGenerated) { this.finalPayslipGenerated = finalPayslipGenerated; }

    public LocalDateTime getFinalPayslipGeneratedAt() { return finalPayslipGeneratedAt; }
    public void setFinalPayslipGeneratedAt(LocalDateTime finalPayslipGeneratedAt) { this.finalPayslipGeneratedAt = finalPayslipGeneratedAt; }

    public Long getFinalPayslipGeneratedBy() { return finalPayslipGeneratedBy; }
    public void setFinalPayslipGeneratedBy(Long finalPayslipGeneratedBy) { this.finalPayslipGeneratedBy = finalPayslipGeneratedBy; }

    public boolean isAccountDeactivated() { return accountDeactivated; }
    public void setAccountDeactivated(boolean accountDeactivated) { this.accountDeactivated = accountDeactivated; }

    public LocalDateTime getAccountDeactivatedAt() { return accountDeactivatedAt; }
    public void setAccountDeactivatedAt(LocalDateTime accountDeactivatedAt) { this.accountDeactivatedAt = accountDeactivatedAt; }

    public Long getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(Long deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
