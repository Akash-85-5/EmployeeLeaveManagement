package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LeaveAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveAttachmentService {

    private final LeaveAttachmentRepository attachmentRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    @Value("${file.upload-dir:uploads/leaves}")
    private String uploadDir;

    // Max file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Allowed types
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/jpg",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public LeaveAttachmentService(LeaveAttachmentRepository attachmentRepository,
                                  LeaveApplicationRepository leaveApplicationRepository) {
        this.attachmentRepository = attachmentRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
    }

    // ── Upload attachments for a leave ───────────────────────────

    @Transactional
    public List<LeaveAttachment> uploadAttachments(Long leaveId,
                                                   Long employeeId,
                                                   MultipartFile[] files) throws IOException {
        // Validate leave exists and belongs to employee
        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new BadRequestException("Leave not found"));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: This leave does not belong to you");
        }

        // Max 5 attachments per leave
        List<LeaveAttachment> existing = attachmentRepository.findByLeaveApplicationId(leaveId);
        if (existing.size() + files.length > 5) {
            throw new BadRequestException(
                    "Maximum 5 attachments allowed per leave. " +
                            "Currently has: " + existing.size());
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        List<LeaveAttachment> savedAttachments = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new BadRequestException(
                        "File type not allowed: " + file.getOriginalFilename() +
                                ". Allowed: JPEG, PNG, PDF, DOC, DOCX");
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BadRequestException(
                        "File too large: " + file.getOriginalFilename() +
                                ". Maximum size is 5MB");
            }

            // Save file with unique name to avoid conflicts
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.write(uploadPath.resolve(uniqueFileName), file.getBytes());

            // Save record
            LeaveAttachment attachment = new LeaveAttachment();
            attachment.setLeaveApplicationId(leaveId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFileUrl(uniqueFileName);
            attachment.setFileType(contentType);
            attachment.setFileSize(file.getSize());

            savedAttachments.add(attachmentRepository.save(attachment));
        }

        return savedAttachments;
    }

    // ── Get all attachments for a leave ──────────────────────────

    public List<LeaveAttachment> getAttachments(Long leaveId) {
        return attachmentRepository.findByLeaveApplicationId(leaveId);
    }

    // ── Delete single attachment ──────────────────────────────────

    @Transactional
    public void deleteAttachment(Long attachmentId, Long employeeId) {
        LeaveAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BadRequestException("Attachment not found"));

        // Verify the leave belongs to employee
        LeaveApplication leave = leaveApplicationRepository
                .findById(attachment.getLeaveApplicationId())
                .orElseThrow(() -> new BadRequestException("Leave not found"));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Unauthorized: Cannot delete this attachment");
        }

        // Delete physical file
        try {
            Path filePath = Paths.get(uploadDir).resolve(attachment.getFileUrl());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail — still remove DB record
        }

        attachmentRepository.delete(attachment);
    }

    // ── Get file path for download ────────────────────────────────

    public Path getFilePath(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }
}