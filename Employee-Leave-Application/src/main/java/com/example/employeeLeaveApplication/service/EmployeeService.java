package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.PersonalDetailsRequest;
import com.example.employeeLeaveApplication.dto.ProfileResponse;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.entity.User;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.enums.Status;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.EmployeePersonalDetailsRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           EmployeePersonalDetailsRepository personalDetailsRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository=userRepository;
        this.personalDetailsRepository = personalDetailsRepository;
    }

    // ==================== EXISTING METHODS ====================

//    public Employee createEmployee(Employee employee) {
//        return employeeRepository.save(employee);
//    }
//    public Employee getEmployee(Long id) {
//        return employeeRepository.findById(id)
//                .orElseThrow(() -> new BadRequestException("Employee not found"));
//    }


    public ProfileResponse getProfile(Long employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        ProfileResponse response = new ProfileResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setManagerId(user.getManagerId());
        response.setActive(user.getStatus() == Status.ACTIVE);
        response.setMustChangePassword(user.isForcePwdChange());
        response.setJoiningDate(user.getJoiningDate());
        response.setBiometricStatus(user.getBiometricStatus().name());
        response.setVpnStatus(user.getVpnStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        if (employee.getManagerId() != null) {
            employeeRepository.findById(employee.getManagerId())
                    .ifPresent(m -> response.setManagerName(m.getName()));
        }

        if (employee.getTeamLeaderId() != null) {
            response.setTeamLeaderId(employee.getTeamLeaderId());
            employeeRepository.findById(employee.getTeamLeaderId())
                    .ifPresent(tl -> response.setTeamLeaderName(tl.getName()));
        }

        Optional<EmployeePersonalDetails> personalOpt =
                personalDetailsRepository.findByEmployeeId(employeeId);

        if (personalOpt.isPresent()) {
            EmployeePersonalDetails pd = personalOpt.get();
            response.setContactNumber(pd.getContactNumber());
            response.setGender(pd.getGender());
            response.setAadharNumber(pd.getAadharNumber());
            response.setPersonalEmail(pd.getPersonalEmail());
            response.setDateOfBirth(pd.getDateOfBirth());
            response.setPresentAddress(pd.getPresentAddress());
            response.setPermanentAddress(pd.getPermanentAddress());
            response.setBloodGroup(pd.getBloodGroup());
            response.setEmergencyContactNumber(pd.getEmergencyContactNumber());
            response.setFatherName(pd.getFatherName());
            response.setMotherName(pd.getMotherName());
            response.setDesignation(pd.getDesignation());
            response.setPersonalDetailsComplete(true);

            if (pd.getSkillSet() != null && !pd.getSkillSet().isBlank()) {
                List<String> skills = Arrays.stream(pd.getSkillSet().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                response.setSkillSet(skills);
            }
        } else {
            response.setPersonalDetailsComplete(false);
        }

        return response;
    }

    @Transactional
    public EmployeePersonalDetails saveOrUpdatePersonalDetails(
            Long employeeId, PersonalDetailsRequest request) {

        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        // Create or update
        EmployeePersonalDetails pd = personalDetailsRepository
                .findByEmployeeId(employeeId)
                .orElse(new EmployeePersonalDetails());

        pd.setEmployeeId(employeeId);
        pd.setContactNumber(request.getContactNumber());
        pd.setGender(request.getGender());
        pd.setAadharNumber(request.getAadharNumber());
        pd.setPersonalEmail(request.getPersonalEmail());
        pd.setDateOfBirth(request.getDateOfBirth());
        pd.setPresentAddress(request.getPresentAddress());
        pd.setPermanentAddress(request.getPermanentAddress());
        pd.setBloodGroup(request.getBloodGroup());
        pd.setMaritalStatus(request.getMaritalStatus());
        pd.setEmergencyContactNumber(request.getEmergencyContactNumber());
        pd.setDesignation(request.getDesignation());
        pd.setSkillSet(request.getSkillSet());
        pd.setFatherName(request.getFatherName());
        pd.setFatherDateOfBirth(request.getFatherDateOfBirth());
        pd.setFatherOccupation(request.getFatherOccupation());
        pd.setFatherAlive(request.getFatherAlive());
        pd.setMotherName(request.getMotherName());
        pd.setMotherDateOfBirth(request.getMotherDateOfBirth());
        pd.setMotherOccupation(request.getMotherOccupation());
        pd.setMotherAlive(request.getMotherAlive());

        return personalDetailsRepository.save(pd);
    }

    public EmployeePersonalDetails getPersonalDetails(Long employeeId) {
        return personalDetailsRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BadRequestException(
                        "Personal details not yet filled for employee: " + employeeId));
    }

    /**
     * Get all employees with filters and pagination
     */
    public Page<Employee> getAllEmployees(
            String name,
            String email,
            String role,
            Long managerId,
            Boolean active,
            Pageable pageable
    ) {
        return employeeRepository.findAll(
                createSpecification(name, email, role, managerId, active),
                pageable
        );
    }

    /**
     * Update employee
     */
    @Transactional
    public Employee updateEmployee(Long id, Employee employee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        if (employee.getName() != null) {
            existing.setName(employee.getName());
        }
        if (employee.getEmail() != null) {
            existing.setEmail(employee.getEmail());
        }
        if (employee.getRole() != null) {
            existing.setRole(employee.getRole());
        }
        if (employee.getManagerId() != null) {
            existing.setManagerId(employee.getManagerId());
        }


        return employeeRepository.save(existing);
    }

    /**
     * Delete/Deactivate employee
     */
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Employee not found"));

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    /**
     * Get team members for a manager
     */
    public List<Employee> getTeamMembers(Long managerId) {
        return employeeRepository.findByManagerId(managerId);
    }

    /**
     * Search employees by name
     */
    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByNameContainingIgnoreCase(query);
    }

    /**
     * Get active employees count
     */
    public Long getActiveEmployeesCount() {
        return employeeRepository.countByActive(true);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create JPA Specification for filtering
     */
    private Specification<Employee> createSpecification(
            String name,
            String email,
            String role,
            Long managerId,
            Boolean active
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (role != null && !role.isEmpty()) {
                predicates.add(cb.equal(root.get("role"), Role.valueOf(role.toUpperCase())));
            }
            if (managerId != null) {
                predicates.add(cb.equal(root.get("managerId"), managerId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}