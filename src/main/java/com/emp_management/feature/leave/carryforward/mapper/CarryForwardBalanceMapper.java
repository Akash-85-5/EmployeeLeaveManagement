package com.emp_management.feature.leave.carryforward.mapper;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.leave.carryforward.dto.CarryForwardBalanceResponse;
import com.emp_management.feature.leave.carryforward.entity.CarryForwardBalance;

public class CarryForwardBalanceMapper {

    /**
     * Maps a CarryForwardBalance entity to a DTO.
     * Use when a balance record exists in DB.
     */
    public static CarryForwardBalanceResponse toDTO(CarryForwardBalance entity) {
        CarryForwardBalanceResponse dto = new CarryForwardBalanceResponse();
        dto.setEmployeeId(entity.getEmployee().getEmpId());
        dto.setEmployeeName(entity.getEmployee().getName());
        dto.setYear(entity.getYear());
        dto.setTotalCarriedForward(entity.getTotalCarriedForward());
        dto.setTotalUsed(entity.getTotalUsed());
        dto.setRemaining(entity.getRemaining());
        return dto;
    }

    /**
     * Builds an empty DTO when no balance record exists yet for the employee.
     * Used in getBalance() when carryForwardRepo returns Optional.empty().
     */
    public static CarryForwardBalanceResponse toEmptyDTO(Employee employee, int year) {
        CarryForwardBalanceResponse dto = new CarryForwardBalanceResponse();
        dto.setEmployeeId(employee.getEmpId());
        dto.setEmployeeName(employee.getName());
        dto.setYear(year);
        dto.setTotalCarriedForward(0.0);
        dto.setTotalUsed(0.0);
        dto.setRemaining(0.0);
        return dto;
    }
}