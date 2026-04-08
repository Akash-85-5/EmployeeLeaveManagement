package com.emp_management.feature.leave.annual.mapper;

import com.emp_management.feature.leave.annual.dto.SickLeaveBalanceResponse;
import com.emp_management.feature.leave.annual.entity.SickLeaveMonthlyBalance;

public class SickLeaveBalanceMapper {

    public static SickLeaveBalanceResponse toDTO(SickLeaveMonthlyBalance entity) {
        SickLeaveBalanceResponse dto = new SickLeaveBalanceResponse();
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setYear(entity.getYear());
        dto.setMonth(entity.getMonth());
        dto.setAvailableDays(entity.getAvailableDays());
        dto.setUsedDays(entity.getUsedDays());
        dto.setRemainingDays(entity.getRemainingDays());
        return dto;
    }
}