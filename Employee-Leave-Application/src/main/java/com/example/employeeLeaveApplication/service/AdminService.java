package com.example.employeeLeaveApplication.service;


import com.example.employeeLeaveApplication.dto.CreateUserRequest;
import com.example.employeeLeaveApplication.dto.UserDropdownResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.enums.Status;
import com.example.employeeLeaveApplication.entity.User;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

    public AdminService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository=employeeRepository;
    }

    // ================= CREATE USER =================

    public void createUser(CreateUserRequest request) {

        // 🔴 EMAIL DUPLICATE CHECK (MANDATORY)
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

        Role role = request.getRole();

        // 🔐 MANAGER ASSIGNMENT RULES
        if (role == Role.EMPLOYEE) {

            if (request.getManagerId() == null) {
                throw new RuntimeException("Manager is required for employee");
            }

            User manager = getUserOrThrow(request.getManagerId());

            if (manager.getRole() != Role.MANAGER) {
                throw new RuntimeException("Assigned manager must be MANAGER");
            }

            user.setManagerId(manager.getId());
        }

        else if (role == Role.MANAGER || role == Role.ADMIN) {

            if (request.getManagerId() == null) {
                throw new RuntimeException("HR must be assigned");
            }

            User hr = getUserOrThrow(request.getManagerId());

            if (hr.getRole() != Role.HR) {
                throw new RuntimeException("Assigned manager must be HR");
            }

            user.setManagerId(hr.getId());
        }

        else if (role == Role.HR) {
            user.setManagerId(null);
        }

        Employee emp = new Employee();
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRole(request.getRole());
        emp.setJoiningDate(request.getJoiningDate());
        emp.setManagerId(request.getManagerId());

        employeeRepository.save(emp);

        userRepository.save(user);
    }

    // ================= DROPDOWN =================

    public List<UserDropdownResponse> getEligibleManagers(Role role) {

        List<User> users;

        if (role == Role.EMPLOYEE) {
            users = userRepository.findByRole(Role.MANAGER);
        } else if (role == Role.MANAGER || role == Role.ADMIN) {
            users = userRepository.findByRole(Role.HR);
        } else {
            return new java.util.ArrayList<>();
        }

        return users.stream()
                .map(u -> new UserDropdownResponse(u.getId(), u.getName()))
                .toList();
    }

    // ================= HELPER =================

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public void resetPassword(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);

        userRepository.save(user);
    }

}