package com.emp_management.infrastructure.storage;


import com.emp_management.feature.leave.annual.entity.LeaveAttachment;
import com.emp_management.feature.leave.annual.repository.LeaveAttachmentRepository;
import com.emp_management.shared.exceptions.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
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
            throw new BadRequestException("Could not create upload folder");
        }
    }

    @GetMapping("/view")
//    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> viewFile(@RequestParam String path) {

        if (path.contains("..")) {
            throw new BadRequestException("Invalid document path.");
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Document not found: " + path);
            }

            String contentType = detectContentType(path);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // "inline" opens in browser, "attachment" forces download
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid document path: " + path);
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

    private String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        return "application/octet-stream";
    }
}