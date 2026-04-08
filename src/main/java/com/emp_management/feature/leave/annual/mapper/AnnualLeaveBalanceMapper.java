package com.emp_management.feature.leave.annual.mapper;

import com.emp_management.feature.leave.annual.dto.AnnualLeaveBalanceResponse;
import com.emp_management.feature.leave.annual.entity.AnnualLeaveMonthlyBalance;

public class AnnualLeaveBalanceMapper {

    public static AnnualLeaveBalanceResponse toDTO(AnnualLeaveMonthlyBalance entity) {
        AnnualLeaveBalanceResponse dto = new AnnualLeaveBalanceResponse();
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setYear(entity.getYear());
        dto.setMonth(entity.getMonth());
        dto.setAvailableDays(entity.getAvailableDays());
        dto.setUsedDays(entity.getUsedDays());
        dto.setRemainingDays(entity.getRemainingDays());
        return dto;
    }
}