package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.OvertimeRequest;
import com.example.employeeLeaveApplication.enums.RequestStatus; // Added this
import com.example.employeeLeaveApplication.enums.CompensationType; // Added this
import com.example.employeeLeaveApplication.repository.OvertimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

@Service
public class OvertimeService {

    @Autowired
    private OvertimeRepository overtimeRepository;

    private final String UPLOAD_DIR = "uploads/overtime/";

    public OvertimeRequest submitRequest(Long empId, String role, OvertimeRequest request, MultipartFile file) throws IOException {

        // 1. Handle File Upload
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            request.setProofDocumentPath(filePath.toString());
        }

        // 2. Logic: Who starts the approval?
        request.setEmployeeId(empId);

        // Fix: Use RequestStatus.VALUE instead of "STRING"
        if ("MANAGER".equalsIgnoreCase(role)) {
            request.setStatus(RequestStatus.PENDING_HR);
        } else if ("TL".equalsIgnoreCase(role)) {
            request.setStatus(RequestStatus.PENDING_MANAGER);
        } else {
            request.setStatus(RequestStatus.PENDING_TL);
        }

        return overtimeRepository.save(request);
    }

    public OvertimeRequest approve(Long requestId, Long approverId, String approverRole) {
        OvertimeRequest req = overtimeRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Fix: Use Enum constants for comparison and assignment
        if ("TL".equalsIgnoreCase(approverRole) && req.getStatus() == RequestStatus.PENDING_TL) {
            req.setStatus(RequestStatus.PENDING_MANAGER);
            req.setTlId(approverId);
            req.setTlDecidedAt(LocalDateTime.now());

        } else if ("MANAGER".equalsIgnoreCase(approverRole) && req.getStatus() == RequestStatus.PENDING_MANAGER) {
            req.setStatus(RequestStatus.PENDING_HR);
            req.setManagerId(approverId);
            req.setManagerDecidedAt(LocalDateTime.now());

        } else if ("HR".equalsIgnoreCase(approverRole) && req.getStatus() == RequestStatus.PENDING_HR) {
            req.setStatus(RequestStatus.APPROVED);
            req.setHrId(approverId);
            req.setHrDecidedAt(LocalDateTime.now());

            // Check if compensation is LEAVE
            if (req.getCompensationType() == CompensationType.LEAVE) {
                updateLeaveBalance(req.getEmployeeId(), req.getTotalHours());
            }
        }

        return overtimeRepository.save(req);
    }

    private void updateLeaveBalance(Long employeeId, Double hours) {
        // TODO: Inject your LeaveBalanceRepository and add hours here
        System.out.println("Adding " + hours + " hours to leave balance for employee: " + employeeId);
    }
}