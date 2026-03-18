package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.LeaveAttachment;
import com.example.employeeLeaveApplication.repository.LeaveAttachmentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final LeaveAttachmentRepository attachmentRepository;

    public FileController(LeaveAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Value("${file.upload-dir:uploads/leaves}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folder", e);
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String filename) throws IOException {

        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

        // ✅ ADDED: Block path traversal attempts
        if (decodedFilename.contains("..") ||
                decodedFilename.contains("/") ||
                decodedFilename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(decodedFilename).normalize();

        // ✅ ADDED: Verify resolved path is still inside upload directory
        if (!filePath.startsWith(uploadPath)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/leave/{leaveId}")
    public List<String> getFilesByLeaveId(@PathVariable Long leaveId) {
        return attachmentRepository.findByLeaveApplicationId(leaveId)
                .stream()
                .map(LeaveAttachment::getFileUrl)
                .collect(Collectors.toList());
    }
}