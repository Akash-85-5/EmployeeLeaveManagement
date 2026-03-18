package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.WorkFromHomeRequest;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.WorkFromHome;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.enums.WfhStatus;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.WorkFromHomeRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkFromHomeService {

    private final WorkFromHomeRepository repository;
    private final EmployeeRepository employeeRepository;

    public WorkFromHomeService(WorkFromHomeRepository repository,
                               EmployeeRepository employeeRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }

    public WorkFromHome applyWFH(Long employeeId, WorkFromHomeRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        WorkFromHome wfh = new WorkFromHome();

        wfh.setEmployeeId(employee.getId());
        wfh.setEmployeeName(employee.getName());

        wfh.setType(request.getType());
        wfh.setStartDate(request.getStartDate());
        wfh.setEndDate(request.getEndDate());
        wfh.setReason(request.getReason());
        wfh.setWorkPlan(request.getWorkPlan());

        Role role = employee.getRole();

        if (role == Role.EMPLOYEE) {
            wfh.setStatus(WfhStatus.PENDING_TEAM_LEADER);
        }
        else if (role == Role.TEAM_LEADER) {
            wfh.setStatus(WfhStatus.PENDING_MANAGER);
        }
        else if (role == Role.MANAGER) {
            wfh.setStatus(WfhStatus.PENDING_HR);
        }

        return repository.save(wfh);
    }
}