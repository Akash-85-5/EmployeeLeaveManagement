package com.emp_management.feature.leave.annual.mapper;

import com.emp_management.feature.leave.annual.dto.LeaveApplicationResponseDTO;
import com.emp_management.feature.leave.annual.dto.LeaveApplicationWithAttachmentsDto;
import com.emp_management.feature.leave.annual.dto.LeaveRemarkDto;
import com.emp_management.feature.leave.annual.entity.LeaveApplication;
import com.emp_management.feature.leave.annual.entity.LeaveApproval;
import com.emp_management.feature.leave.annual.entity.LeaveAttachment;

import java.util.List;


public class LeaveApplicationWithAttachmentsDtoMapper {

    public static LeaveApplicationWithAttachmentsDto toDTO(LeaveApplication leaveApplication , List<LeaveAttachment> leaveAttachments, List<LeaveApproval> leaveRemarks){
        if(leaveApplication == null) return null;
        LeaveApplicationWithAttachmentsDto dto = new LeaveApplicationWithAttachmentsDto();

        LeaveApplicationResponseDTO leave = LeaveApplicationMapper.toDTO(leaveApplication);

        List<LeaveRemarkDto> remarks = LeaveApplicationMapper.mapToRemarks(leaveRemarks);

        dto.setLeaveApplicationResponseDTO(leave);
        dto.setAttachments(leaveAttachments);
        dto.setRemarks(remarks);
        dto.setAttachmentCount(leaveAttachments.size());
        return dto;
    }
}
