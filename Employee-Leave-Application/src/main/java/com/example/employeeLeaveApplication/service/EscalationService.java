package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscalationService {

    private final LeaveApplicationRepository leaveRepo;
    private final EmployeeRepository employeeRepo;

    private static final int SLA_HOURS = 24;

    public EscalationService(LeaveApplicationRepository leaveRepo,
                             EmployeeRepository employeeRepo) {
        this.leaveRepo = leaveRepo;
        this.employeeRepo = employeeRepo;
    }

    public void escalatePendingLeaves() {

        LocalDateTime deadline =
                LocalDateTime.now().minusHours(SLA_HOURS);

        List<LeaveApplication> overdueLeaves =
                leaveRepo.findByStatusAndSubmittedAtBeforeAndEscalatedFalse(
                        LeaveStatus.PENDING,
                        deadline
                );
        List<Employee> hrList = employeeRepo.findByRole(Role.HR);

        if (hrList.isEmpty()) {
            throw new RuntimeException("No HR found");
        }

        Employee hr = hrList.get(0); // take first HR

        for (LeaveApplication leave : overdueLeaves) {

            leave.setEscalated(true);
            leave.setEscalatedAt(LocalDateTime.now());
            leave.setManagerId(hr.getId());

            leaveRepo.save(leave);
        }
    }
}
