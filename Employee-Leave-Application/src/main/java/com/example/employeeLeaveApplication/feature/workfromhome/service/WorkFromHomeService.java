package com.example.employeeLeaveApplication.feature.workfromhome.service;

import com.example.employeeLeaveApplication.feature.workfromhome.dto.WorkFromHomeRequest;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.workfromhome.entity.WorkFromHome;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.shared.enums.WfhStatus;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.workfromhome.repository.WorkFromHomeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkFromHomeService {

    private final WorkFromHomeRepository repository;
    private final EmployeeRepository employeeRepository;

    public WorkFromHomeService(WorkFromHomeRepository repository,
                               EmployeeRepository employeeRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }
    public List<WorkFromHome> getEmployeeWFH(Long employeeId) {
        return repository.findByEmployeeId(employeeId);
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
    public List<WorkFromHome> getPendingForTL() {
        return repository.findByStatus(WfhStatus.PENDING_TEAM_LEADER);
    }

    public List<WorkFromHome> getPendingForManager() {
        return repository.findByStatus(WfhStatus.PENDING_MANAGER);
    }

// HR Pending

    public List<WorkFromHome> getPendingForHR() {
        return repository.findByStatus(WfhStatus.PENDING_HR);
    }
//approve ---


    public WorkFromHome approveByTL(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.PENDING_MANAGER);

        return repository.save(wfh);
    }
    public WorkFromHome approveByManager(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.PENDING_HR);

        return repository.save(wfh);
    }

    public WorkFromHome approveByHR(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.APPROVED);

        return repository.save(wfh);
    }
    // TL Reject----

    public WorkFromHome rejectByTL(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.REJECTED);

        return repository.save(wfh);
    }


    // Manager Reject
    public WorkFromHome rejectByManager(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.REJECTED);

        return repository.save(wfh);
    }


    // HR Reject
    public WorkFromHome rejectByHR(Long id) {

        WorkFromHome wfh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WFH not found"));

        wfh.setStatus(WfhStatus.REJECTED);

        return repository.save(wfh);
    }


}