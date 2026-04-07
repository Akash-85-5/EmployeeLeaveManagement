package com.example.employeeLeaveApplication.feature.noticePeriod.dto;

import com.example.employeeLeaveApplication.feature.noticePeriod.entity.EmployeeSeparation;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import com.example.employeeLeaveApplication.shared.enums.TerminationGrounds;
import com.example.employeeLeaveApplication.shared.enums.TerminationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SeparationResponse {

    private Long               id;
    private Long               employeeId;
    private String             employeeName;
    private String             employeeRole;
    private Long               initiatorId;
    private String             initiatorRole;
    private SeparationType     separationType;
    private TerminationType    terminationType;
    private TerminationGrounds terminationGrounds;
    private String             reason;
    private LocalDate          deathDate;
    private SeparationStatus   status;

    // Approval trail
    private Long          approvedByManager;
    private LocalDateTime managerActionAt;
    private String        managerRemarks;
    private Long          approvedByHr;
    private LocalDateTime hrActionAt;
    private String        hrRemarks;
    private Long          approvedByCeo;
    private LocalDateTime ceoActionAt;
    private String        ceoRemarks;

    // Notice period
    private LocalDate noticePeriodStart;
    private LocalDate noticePeriodEnd;
    private int       lopDaysInNotice;

    // Exit checklist
    private boolean       laptopReturned;
    private boolean       idCardReturned;
    private boolean       ktCompleted;
    private LocalDateTime exitChecklistCompletedAt;

    // Payslip
    private boolean       finalPayslipGenerated;
    private LocalDateTime finalPayslipGeneratedAt;

    // Account
    private boolean       accountDeactivated;
    private LocalDateTime accountDeactivatedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Static factory — converts entity to response ──────────────
    public static SeparationResponse from(EmployeeSeparation s) {
        SeparationResponse r = new SeparationResponse();
        r.id                       = s.getId();
        r.employeeId               = s.getEmployeeId();
        r.employeeName             = s.getEmployeeName();
        r.employeeRole             = s.getEmployeeRole();
        r.initiatorId              = s.getInitiatorId();
        r.initiatorRole            = s.getInitiatorRole();
        r.separationType           = s.getSeparationType();
        r.terminationType          = s.getTerminationType();
        r.terminationGrounds       = s.getTerminationGrounds();
        r.reason                   = s.getReason();
        r.deathDate                = s.getDeathDate();
        r.status                   = s.getStatus();
        r.approvedByManager        = s.getApprovedByManager();
        r.managerActionAt          = s.getManagerActionAt();
        r.managerRemarks           = s.getManagerRemarks();
        r.approvedByHr             = s.getApprovedByHr();
        r.hrActionAt               = s.getHrActionAt();
        r.hrRemarks                = s.getHrRemarks();
        r.approvedByCeo            = s.getApprovedByCeo();
        r.ceoActionAt              = s.getCeoActionAt();
        r.ceoRemarks               = s.getCeoRemarks();
        r.noticePeriodStart        = s.getNoticePeriodStart();
        r.noticePeriodEnd          = s.getNoticePeriodEnd();
        r.lopDaysInNotice          = s.getLopDaysInNotice();
        r.laptopReturned           = s.isLaptopReturned();
        r.idCardReturned           = s.isIdCardReturned();
        r.ktCompleted              = s.isKtCompleted();
        r.exitChecklistCompletedAt = s.getExitChecklistCompletedAt();
        r.finalPayslipGenerated    = s.isFinalPayslipGenerated();
        r.finalPayslipGeneratedAt  = s.getFinalPayslipGeneratedAt();
        r.accountDeactivated       = s.isAccountDeactivated();
        r.accountDeactivatedAt     = s.getAccountDeactivatedAt();
        r.createdAt                = s.getCreatedAt();
        r.updatedAt                = s.getUpdatedAt();
        return r;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long               getId()                       { return id; }
    public Long               getEmployeeId()               { return employeeId; }
    public String             getEmployeeName()             { return employeeName; }
    public String             getEmployeeRole()             { return employeeRole; }
    public Long               getInitiatorId()              { return initiatorId; }
    public String             getInitiatorRole()            { return initiatorRole; }
    public SeparationType     getSeparationType()           { return separationType; }
    public TerminationType    getTerminationType()          { return terminationType; }
    public TerminationGrounds getTerminationGrounds()       { return terminationGrounds; }
    public String             getReason()                   { return reason; }
    public LocalDate          getDeathDate()                { return deathDate; }
    public SeparationStatus   getStatus()                   { return status; }
    public Long               getApprovedByManager()        { return approvedByManager; }
    public LocalDateTime      getManagerActionAt()          { return managerActionAt; }
    public String             getManagerRemarks()           { return managerRemarks; }
    public Long               getApprovedByHr()             { return approvedByHr; }
    public LocalDateTime      getHrActionAt()               { return hrActionAt; }
    public String             getHrRemarks()                { return hrRemarks; }
    public Long               getApprovedByCeo()            { return approvedByCeo; }
    public LocalDateTime      getCeoActionAt()              { return ceoActionAt; }
    public String             getCeoRemarks()               { return ceoRemarks; }
    public LocalDate          getNoticePeriodStart()        { return noticePeriodStart; }
    public LocalDate          getNoticePeriodEnd()          { return noticePeriodEnd; }
    public int                getLopDaysInNotice()          { return lopDaysInNotice; }
    public boolean            isLaptopReturned()            { return laptopReturned; }
    public boolean            isIdCardReturned()            { return idCardReturned; }
    public boolean            isKtCompleted()               { return ktCompleted; }
    public LocalDateTime      getExitChecklistCompletedAt() { return exitChecklistCompletedAt; }
    public boolean            isFinalPayslipGenerated()     { return finalPayslipGenerated; }
    public LocalDateTime      getFinalPayslipGeneratedAt()  { return finalPayslipGeneratedAt; }
    public boolean            isAccountDeactivated()        { return accountDeactivated; }
    public LocalDateTime      getAccountDeactivatedAt()     { return accountDeactivatedAt; }
    public LocalDateTime      getCreatedAt()                { return createdAt; }
    public LocalDateTime      getUpdatedAt()                { return updatedAt; }
}