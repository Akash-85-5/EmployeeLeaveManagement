package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.entity.LopRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a single LOP record.
 * Used by detail endpoints for all roles.
 */
public class LopResponse {

    private Long          id;
    private Long          employeeId;
    private String        employeeName;
    private String        employeeRole;
    private LocalDate     lopDate;
    private Double        lopDays;
    private Integer       lopMonth;
    private Integer       lopYear;
    private String        lopReason;
    private boolean       reversed;
    private String        reversalReason;
    private LocalDateTime createdAt;

    // Static factory — convert entity to DTO
    public static LopResponse from(LopRecord e) {
        LopResponse r    = new LopResponse();
        r.id             = e.getId();
        r.employeeId     = e.getEmployeeId();
        r.employeeName   = e.getEmployeeName();
        r.employeeRole   = e.getEmployeeRole();
        r.lopDate        = e.getLopDate();
        r.lopDays        = e.getLopDays();
        r.lopMonth       = e.getLopMonth();
        r.lopYear        = e.getLopYear();
        r.lopReason      = e.getLopReason();
        r.reversed       = e.isReversed();
        r.reversalReason = e.getReversalReason();
        r.createdAt      = e.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(String employeeRole) { this.employeeRole = employeeRole; }

    public LocalDate getLopDate() { return lopDate; }
    public void setLopDate(LocalDate lopDate) { this.lopDate = lopDate; }

    public Double getLopDays() { return lopDays; }
    public void setLopDays(Double lopDays) { this.lopDays = lopDays; }

    public Integer getLopMonth() { return lopMonth; }
    public void setLopMonth(Integer lopMonth) { this.lopMonth = lopMonth; }

    public Integer getLopYear() { return lopYear; }
    public void setLopYear(Integer lopYear) { this.lopYear = lopYear; }

    public String getLopReason() { return lopReason; }
    public void setLopReason(String lopReason) { this.lopReason = lopReason; }

    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }

    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}