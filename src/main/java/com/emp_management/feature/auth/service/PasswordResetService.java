package com.emp_management.feature.auth.service;

import com.emp_management.feature.auth.dto.PasswordResetAdminResponse;
import com.emp_management.feature.auth.entity.PasswordResetRequest;
import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.PasswordResetRequestRepository;
import com.emp_management.feature.auth.repository.UserRepository;
import com.emp_management.shared.enums.ResetStatus;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordResetService {

    private final PasswordResetRequestRepository resetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            PasswordResetRequestRepository resetRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.resetRepository = resetRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= USER REQUEST =================

    public void requestReset(String email) {

        User user = userRepository.findByEmployee_Email(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyPending =
                resetRepository.findByUserIdAndStatus(
                        user.getEmployee().getEmpId(),
                        ResetStatus.PENDING
                ).isPresent();

        if (alreadyPending) {
            throw new BadRequestException("Reset request already pending");
        }

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUserId(user.getEmployee().getEmpId());
        request.setStatus(ResetStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        resetRepository.save(request);
    }

    // ================= ADMIN APPROVE =================

    public void approveResetByEmail(Long requestId, String adminEmail) {

        PasswordResetRequest request =
                resetRepository.findById(requestId)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Request not found"));

        if (request.getStatus() != ResetStatus.PENDING) {
            throw new BadRequestException("Already handled");
        }

        User admin = userRepository.findByEmployee_Email(adminEmail)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found"));

        User user = userRepository.findByEmployee_EmpId(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode("1234"));
        user.setForcePwdChange(true);
        userRepository.save(user);

        request.setStatus(ResetStatus.COMPLETED);
        request.setHandledBy(admin.getId());
        request.setHandledAt(LocalDateTime.now());

        resetRepository.save(request);
    }

    // ================= ADMIN REJECT =================

    public void rejectResetByEmail(Long requestId, String adminEmail) {

        PasswordResetRequest request =
                resetRepository.findById(requestId)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Request not found"));

        if (request.getStatus() != ResetStatus.PENDING) {
            throw new BadRequestException("Already handled");
        }

        User admin = userRepository.findByEmployee_Email(adminEmail)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found"));

        request.setStatus(ResetStatus.REJECTED);
        request.setHandledBy(admin.getId());
        request.setHandledAt(LocalDateTime.now());

        resetRepository.save(request);
    }

    // ================= ADMIN LIST =================

    public List<PasswordResetAdminResponse> getByStatus(ResetStatus status) {

        List<PasswordResetRequest> requests =
                resetRepository.findByStatus(status);

        return requests.stream().map(req -> {

            User user = userRepository.findByEmployee_EmpId(req.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            return new PasswordResetAdminResponse(
                    req.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    req.getRequestedAt(),
                    req.getStatus().name()
            );

        }).toList();
    }
}
