package com.emp_management.feature.employee.mapper;

import com.emp_management.feature.employee.dto.EmployeeResponseDTO;
import com.emp_management.feature.employee.entity.Employee;

public class EmployeeMapper {

    public static EmployeeResponseDTO toDTO(Employee e) {

        EmployeeResponseDTO dto = new EmployeeResponseDTO();

        dto.setEmpId(e.getEmpId());
        dto.setName(e.getName());
        dto.setEmail(e.getEmail());
        dto.setReportingId(e.getReportingId());
        dto.setActive(e.isActive());
        dto.setTeamId(e.getTeamId());

        dto.setEmployeeExperience(
                e.getEmployeeExperience() != null ? e.getEmployeeExperience().name() : null
        );

        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());

        // ✅ Role
        dto.setRoleName(
                e.getRole() != null ? e.getRole().getRoleName() : null
        );

        // ✅ Department
        dto.setDepartmentName(
                e.getDepartment() != null ? e.getDepartment().getDepartmentName() : null
        );

        // ✅ Branch
        dto.setBranchName(
                e.getBranch() != null ? e.getBranch().getName() : null
        );

        // ✅ Onboarding (NO recursion)
        if (e.getOnboarding() != null) {
            dto.setJoiningDate(e.getOnboarding().getJoiningDate());
            dto.setBiometricStatus(
                    e.getOnboarding().getBiometricStatus() != null
                            ? e.getOnboarding().getBiometricStatus().name()
                            : null
            );
            dto.setVpnStatus(
                    e.getOnboarding().getVpnStatus() != null
                            ? e.getOnboarding().getVpnStatus().name()
                            : null
            );
            dto.setOnboardingCompletedAt(
                    e.getOnboarding().getOnboardingCompletedAt()
            );
        }

        return dto;
    }
}
