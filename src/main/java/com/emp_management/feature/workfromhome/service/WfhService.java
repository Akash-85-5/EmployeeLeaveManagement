package com.emp_management.feature.workfromhome.service;

import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.workfromhome.entity.WfhRequest;
import com.emp_management.feature.workfromhome.repository.WfhRepository;
import com.emp_management.shared.enums.HalfDayType;
import com.emp_management.shared.enums.RequestStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import com.emp_management.shared.exceptions.ResourceNotFoundException;
import com.emp_management.shared.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class WfhService {

    private final WfhRepository wfhRepository;
    private final EmployeeRepository employeeRepository;

    public WfhService(WfhRepository wfhRepository,
                      EmployeeRepository employeeRepository) {
        this.wfhRepository = wfhRepository;
        this.employeeRepository = employeeRepository;
    }
    public List<WfhRequest> getEmployeeHistory(String empId) {
        return wfhRepository.findByEmployee_EmpId(empId);
    }

    public List<WfhRequest> getManagerHistory(String managerId) {
        return wfhRepository.findByEmployee_ReportingId(managerId);
    }

    // APPLY (same as leave logic)
    public WfhRequest apply(String empId,
                            LocalDate from,
                            LocalDate to,
                            String reason,
                            HalfDayType halfDayType) {

        Employee emp = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // ✅ Step 1: Basic validations
        if (from.isAfter(to)) {
            throw new BadRequestException("From date cannot be after To date");
        }

        if (from.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot apply for past dates");
        }

        if (halfDayType != null && !from.equals(to)) {
            throw new BadRequestException("Half-day only allowed for single day");
        }

        // 🔥 ✅ Step 2: ADD OVERLAP CHECK HERE
        List<WfhRequest> existing = wfhRepository
                .findByEmployee_EmpIdAndStatusIn(
                        empId,
                        List.of(RequestStatus.PENDING, RequestStatus.APPROVED)
                );

        for (WfhRequest req : existing) {

            boolean isSameDay = from.equals(req.getFromDate());

            //  FULL DAY conflict
            if (req.getHalfDayType() == null &&
                    !(to.isBefore(req.getFromDate()) || from.isAfter(req.getToDate()))) {
                throw new BadRequestException("WFH already exists for selected dates");
            }

            // HALF DAY conflict
            if (isSameDay && req.getHalfDayType() != null) {
                if (halfDayType == null || halfDayType == req.getHalfDayType()) {
                    throw new BadRequestException("WFH conflict for same half");
                }
            }
        }

        // Step 3: Create request
        WfhRequest wfh = new WfhRequest();
        wfh.setEmployee(emp);
        wfh.setFromDate(from);
        wfh.setToDate(to);
        wfh.setReason(reason);
        wfh.setHalfDayType(halfDayType);

        wfh.setStatus(RequestStatus.PENDING);
        wfh.setCurrentApprover(emp.getReportingId());

        return wfhRepository.save(wfh);
    }

    //  APPROVE (2-level flow)
    public WfhRequest approve(Long id, String approverId) {

        WfhRequest wfh = wfhRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("WFH not found"));

        if (!approverId.equals(wfh.getCurrentApprover())) {
            throw new UnauthorizedException("Not authorized");
        }

        Employee emp = wfh.getEmployee();

        // RM1 → RM2
        Employee rm1 = employeeRepository.findByEmpId(emp.getReportingId()).orElse(null);

        if (rm1 != null && rm1.getReportingId() != null &&
                approverId.equals(emp.getReportingId())) {

            wfh.setCurrentApprover(rm1.getReportingId());
            wfh.setStatus(RequestStatus.PENDING);

        } else {
            // Final
            wfh.setStatus(RequestStatus.APPROVED);
            wfh.setCurrentApprover(null);
        }

        return wfhRepository.save(wfh);
    }

    // REJECT
    public WfhRequest reject(Long id, String approverId, String remarks) {

        WfhRequest wfh = wfhRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("WFH not found"));

        if (!approverId.equals(wfh.getCurrentApprover())) {
            throw new UnauthorizedException("Not authorized");
        }

        wfh.setStatus(RequestStatus.REJECTED);
        wfh.setCurrentApprover(null);
        wfh.setRemarks(remarks);

        return wfhRepository.save(wfh);
    }
    public String cancelWfh(Long id, String employeeId) {

        WfhRequest wfh = wfhRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("WFH request not found"));

        // Check ownership
        if (!wfh.getEmployee().getEmpId().equals(employeeId)) {
            throw new RuntimeException("You can only cancel your own request");
        }

        // Check status
        if (wfh.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only PENDING requests can be cancelled");
        }

        // Cancel
        wfh.setStatus(RequestStatus.CANCELLED);
        wfhRepository.save(wfh);

        return "WFH request cancelled successfully";
    }
    //  EMPLOYEE VIEW
    public List<WfhRequest> getMyRequests(String empId) {
        return wfhRepository.findByEmployee_EmpId(empId);
    }

    // MANAGER VIEW
    public List<WfhRequest> getPending(String approverId) {
        return wfhRepository.findByCurrentApprover(approverId);
    }
}
