package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.ODRequest;
import com.example.employeeLeaveApplication.enums.ODStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.exceptions.ResourceNotFoundException;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.ODRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ODService {

    private final ODRequestRepository odRepository;
    private final EmployeeRepository employeeRepository;

    public ODService(ODRequestRepository odRepository,
                     EmployeeRepository employeeRepository) {
        this.odRepository = odRepository;
        this.employeeRepository = employeeRepository;
    }

    // ✅ Create OD Request with Overlap Check
    public ODRequest createOD(Long employeeId, ODRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // 🔥 NEW: Prevent overlapping OD requests for the same person
        // This checks if any existing PENDING or APPROVED OD clashing with these dates
        List<ODRequest> overlappingODs = odRepository.findOverlappingODs(
                employeeId,
                request.getFromDate(),
                request.getToDate()
        );

        if (!overlappingODs.isEmpty()) {
            throw new BadRequestException("You already have an active OD request that overlaps with these dates.");
        }

        request.setEmployeeId(employeeId);

        switch (employee.getRole()) {
            case EMPLOYEE:
                request.setStatus(ODStatus.PENDING_TEAM_LEADER);
                break;
            case TEAM_LEADER:
                request.setStatus(ODStatus.PENDING_MANAGER);
                break;
            case MANAGER:
            case ADMIN:
                request.setStatus(ODStatus.PENDING_HR);
                break;
            default:
                throw new BadRequestException("HR cannot create OD request");
        }

        return odRepository.save(request);
    }

    // ✅ Approve OD
    public ODRequest approveOD(Long odId, Long approverId) {
        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        switch (od.getStatus()) {
            case PENDING_TEAM_LEADER:
                if (approver.getRole() != Role.TEAM_LEADER)
                    throw new BadRequestException("Only Team Leader can approve");
                od.setStatus(ODStatus.PENDING_MANAGER);
                break;

            case PENDING_MANAGER:
                if (approver.getRole() != Role.MANAGER)
                    throw new BadRequestException("Only Team Manager can approve");
                od.setStatus(ODStatus.APPROVED);
                break;

            case PENDING_HR:
                if (approver.getRole() != Role.HR)
                    throw new BadRequestException("Only HR can approve");
                od.setStatus(ODStatus.APPROVED);
                break;

            default:
                throw new BadRequestException("OD already processed");
        }

        return odRepository.save(od);
    }

    // ❌ Reject OD
    public ODRequest rejectOD(Long odId, Long approverId) {
        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        od.setStatus(ODStatus.REJECTED);
        return odRepository.save(od);
    }

    // ❌ Cancel OD Request
    public ODRequest cancelOD(Long odId, Long userId) {
        Employee user = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ODRequest od = odRepository.findById(odId)
                .orElseThrow(() -> new ResourceNotFoundException("OD not found"));

        if (od.getStatus() == ODStatus.REJECTED || od.getStatus() == ODStatus.CANCELLED) {
            throw new BadRequestException("OD already finalized");
        }

        if (od.getStatus() == ODStatus.APPROVED) {
            if (user.getRole() != Role.HR) {
                throw new BadRequestException("Only HR can cancel approved OD");
            }
            od.setStatus(ODStatus.CANCELLED);
            return odRepository.save(od);
        }

        if (od.getEmployeeId().equals(userId)) {
            od.setStatus(ODStatus.CANCELLED);
            return odRepository.save(od);
        }

        if (user.getRole() == Role.TEAM_LEADER && od.getStatus() == ODStatus.PENDING_TEAM_LEADER) {
            od.setStatus(ODStatus.CANCELLED);
            return odRepository.save(od);
        }

        if (user.getRole() == Role.MANAGER && od.getStatus() == ODStatus.PENDING_MANAGER) {
            od.setStatus(ODStatus.CANCELLED);
            return odRepository.save(od);
        }

        if (user.getRole() == Role.HR && od.getStatus() == ODStatus.PENDING_HR) {
            od.setStatus(ODStatus.CANCELLED);
            return odRepository.save(od);
        }

        throw new BadRequestException("You are not allowed to cancel this OD request");
    }
}