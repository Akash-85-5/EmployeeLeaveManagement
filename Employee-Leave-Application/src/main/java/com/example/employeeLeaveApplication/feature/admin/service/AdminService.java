package com.example.employeeLeaveApplication.feature.admin.service;

import com.example.employeeLeaveApplication.feature.admin.dto.CreateUserRequest;
import com.example.employeeLeaveApplication.feature.admin.dto.UserDropdownResponse;
import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.auth.entity.User;
import com.example.employeeLeaveApplication.feature.leave.annual.service.LeaveAllocationService;
import com.example.employeeLeaveApplication.shared.enums.Role;
import com.example.employeeLeaveApplication.shared.enums.Status;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationService leaveAllocationService;

    public AdminService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmployeeRepository employeeRepository,
                        LeaveAllocationService leaveAllocationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
        this.leaveAllocationService = leaveAllocationService;
    }

    @Transactional
    public void createUser(CreateUserRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setJoiningDate(request.getJoiningDate());
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        user.setStatus(Status.ACTIVE);
        user.setReportingId(request.getReportingId());

        Role role = request.getRole();

        Employee emp = new Employee();
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRole(request.getRole());
        emp.setJoiningDate(request.getJoiningDate());
        emp.setReportingId(request.getReportingId());

        Employee savedEmp = employeeRepository.save(emp);
        userRepository.save(user);

        if ((role != Role.CEO) || (role != Role.CFO)) {
            try {
                leaveAllocationService.allocateForNewEmployee(savedEmp.getId());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create leave allocations: " + e.getMessage());
            }
        }
    }
    public List<UserDropdownResponse> getEligibleManagers(Role role) {
        List<User> users;
        if (role == Role.EMPLOYEE) {
            users = userRepository.findByRole(Role.MANAGER);
        } else if (role == Role.MANAGER || role == Role.ADMIN) {
            users = userRepository.findByRole(Role.HR);
        } else {
            return new ArrayList<>();
        }
        return users.stream()
                .map(u -> new UserDropdownResponse(u.getId(), u.getName()))
                .toList();
    }

    public void resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        userRepository.save(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}