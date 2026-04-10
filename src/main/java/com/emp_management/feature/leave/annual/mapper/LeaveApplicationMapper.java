package com.emp_management.feature.leave.annual.mapper;

import com.emp_management.feature.leave.annual.dto.LeaveApplicationResponseDTO;
import com.emp_management.feature.leave.annual.dto.LeaveRemarkDto;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveApproval;

import java.util.List;
import java.util.stream.Collectors;

public class LeaveApplicationMapper {

    public static LeaveApplicationResponseDTO toDTO(LeaveApplication entity) {
        if (entity == null) return null;

        LeaveApplicationResponseDTO dto = new LeaveApplicationResponseDTO();

        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId()); // uses the @Transient helper you already have
        dto.setLeaveTypeName(
                entity.getLeaveType() != null ? entity.getLeaveType().getLeaveType() : null
        );
        dto.setStartDateHalfDayType(entity.getStartDateHalfDayType());
        dto.setEndDateHalfDayType(entity.getEndDateHalfDayType());
        dto.setIsAppointment(entity.getIsAppointment());
        dto.setYear(entity.getYear());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDays(entity.getDays());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setCurrentApprovalLevel(entity.getCurrentApprovalLevel());
        dto.setRequiredApprovalLevels(entity.getRequiredApprovalLevels());
        dto.setCurrentApproverId(entity.getCurrentApproverId());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setFirstApproverId(entity.getFirstApproverId());
        dto.setFirstApproverDecision(entity.getFirstApproverDecision());
        dto.setFirstApproverDecidedAt(entity.getFirstApproverDecidedAt());
        dto.setSecondApproverId(entity.getSecondApproverId());
        dto.setSecondApproverDecision(entity.getSecondApproverDecision());
        dto.setSecondApproverDecidedAt(entity.getSecondApproverDecidedAt());
        dto.setApprovedBy(entity.getApprovedBy());
        dto.setApprovedRole(entity.getApprovedRole());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setCarryForwardUsed(entity.getCarryForwardUsed());
        dto.setCompOffUsed(entity.getCompOffUsed());
        dto.setLossOfPayApplied(entity.getLossOfPayApplied());
        dto.setPendingLopDays(entity.getPendingLopDays());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEscalated(entity.getEscalated());
        dto.setEscalatedAt(entity.getEscalatedAt());

        return dto;
    }

    // ADD THIS NEW METHOD
    public static List<LeaveRemarkDto> mapToRemarks(List<LeaveApproval> approvals) {
        if (approvals == null) return List.of();
        return approvals.stream()
                .map(LeaveRemarkDto::fromApproval)
                .collect(Collectors.toList());
    }
}