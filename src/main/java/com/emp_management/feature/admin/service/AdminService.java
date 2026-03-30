package com.emp_management.feature.admin.service;


import com.emp_management.feature.admin.dto.CreateUserRequest;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import com.emp_management.feature.employee.repository.EmployeeOnboardingRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.BiometricVpnStatus;
import com.emp_management.shared.enums.EmployeeStatus;
import com.emp_management.shared.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationService leaveAllocationService;
    private final RoleRepository roleRepository;,
    private final EmployeeOnboardingRepository employeeOnboardingRepository;

    public AdminService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmployeeRepository employeeRepository,
                        LeaveAllocationService leaveAllocationService,
                        RoleRepository roleRepository,
                        EmployeeOnboardingRepository employeeOnboardingRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
        this.leaveAllocationService = leaveAllocationService;
        this.roleRepository = roleRepository;
        this.employeeOnboardingRepository=employeeOnboardingRepository;
    }

    @Transactional
    public void createUser(CreateUserRequest request) {

        if (userRepository.findByEmployee_Email(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findById(request.getRole())
                .orElseThrow(()-> new RuntimeException("Role not fount"));

        // Save Employee first (main entity)
        Employee emp = new Employee();
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRole(role);
        emp.setReportingId(request.getReportingId());
        emp.setEmployeeExperience(request.getEmployeeExperience());
        Employee savedEmp = employeeRepository.save(emp);

        EmployeeOnboarding EO = new EmployeeOnboarding();
        EO.setEmployee(savedEmp);
        EO.setJoiningDate(request.getJoiningDate());
        EO.setBiometricStatus(BiometricVpnStatus.PENDING);
        EO.setVpnStatus(BiometricVpnStatus.PENDING);
        employeeOnboardingRepository.save(EO);


        // Link User to Employee
        User user = new User();
        user.setEmployee(savedEmp);
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        user.setStatus(EmployeeStatus.ACTIVE);
        userRepository.save(user);

        // Fixed || → && bug from before
        Long roleId = request.getRole();
        if (roleId != 1 && roleId != 2) {
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