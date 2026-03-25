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

        if (userRepository.findByEmployee_Email(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Save Employee first (main entity)
        Employee emp = new Employee();
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRole(request.getRole());
        emp.setJoiningDate(request.getJoiningDate());
        emp.setManagerId(request.getManagerId());
        emp.setEmployeeExperience(request.getEmployeeExperience());
        Employee savedEmp = employeeRepository.save(emp);

        // Link User to Employee
        User user = new User();
        user.setEmployee(savedEmp);               // FK link
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        // Fixed || → && bug from before
        Role role = request.getRole();
        if (role != Role.CEO && role != Role.CFO) {
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
            users = userRepository.findByEmployee_Role(Role.MANAGER);
        } else if (role == Role.MANAGER || role == Role.ADMIN) {
            users = userRepository.findByEmployee_Role(Role.HR);
        } else {
            return new ArrayList<>();
        }
        return users.stream()
                .map(u -> new UserDropdownResponse(u.getId(), u.getName()))
                .toList();
    }

    public void resetPassword(Long userId) {
        User user = userRepository.findByEmployee_Id(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        userRepository.save(user);
    }
}