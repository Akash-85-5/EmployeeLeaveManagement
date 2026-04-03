package com.emp_management.feature.admin.service;


import com.emp_management.feature.admin.dto.CreateUserRequest;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.feature.employee.entity.Employee;
import com.emp_management.feature.employee.entity.EmployeeOnboarding;
import com.emp_management.feature.employee.repository.EmployeeOnboardingRepository;
import com.emp_management.feature.employee.repository.EmployeeRepository;
import com.emp_management.feature.leave.annual.service.LeaveAllocationService;
import com.emp_management.shared.entity.Branch;
import com.emp_management.shared.entity.Department;
import com.emp_management.shared.entity.Role;
import com.emp_management.shared.enums.BiometricVpnStatus;
import com.emp_management.shared.enums.EmployeeStatus;
import com.emp_management.shared.repository.BranchRepository;
import com.emp_management.shared.repository.DepartmentRepository;
import com.emp_management.shared.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationService leaveAllocationService;
    private final RoleRepository roleRepository;
    private final EmployeeOnboardingRepository employeeOnboardingRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;

    public AdminService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmployeeRepository employeeRepository,
                        LeaveAllocationService leaveAllocationService,
                        RoleRepository roleRepository,
                        EmployeeOnboardingRepository employeeOnboardingRepository,
                        DepartmentRepository departmentRepository,
                        BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
        this.leaveAllocationService = leaveAllocationService;
        this.roleRepository = roleRepository;
        this.employeeOnboardingRepository=employeeOnboardingRepository;
        this.departmentRepository=departmentRepository;
        this.branchRepository=branchRepository;
    }

    @Transactional
    public void createUser(CreateUserRequest request) {

        if (userRepository.findByEmployee_Email(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(()-> new EntityNotFoundException("Role not fount"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found: " + request.getDepartmentId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + request.getBranchId()));

        Employee emp = new Employee();
        emp.setEmpId(request.getEmpId());
        emp.setName(request.getName());
        emp.setEmail(request.getEmail());
        emp.setRole(role);
        emp.setDepartment(department);
        emp.setBranch(branch);
        emp.setTeamId(request.getTeamId());
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

//        // Fixed || → && bug from before
//        Long roleId = request.getRoleId();
//        if (roleId != 1 && roleId != 2) {
//            try {
//                leaveAllocationService.allocateForNewEmployee(savedEmp.getEmpId());
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to create leave allocations: " + e.getMessage());
//            }
//        }
    }



    public void resetPassword(String userId) {
        User user = userRepository.findByEmployee_EmpId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        userRepository.save(user);
    }
}