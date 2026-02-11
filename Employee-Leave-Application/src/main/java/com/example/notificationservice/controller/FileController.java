package com.example.notificationservice.controller;

import com.example.notificationservice.entity.LeaveAttachment;
import com.example.notificationservice.repository.LeaveAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
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

    // DOWNLOAD API: Allows clicking a link to view/download the file
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        Path filePath = Paths.get(uploadDir).resolve(decodedFilename).normalize();

        if (!Files.exists(filePath)) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(filePath.toUri());

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // LIST API: Returns all file URLs for a specific leave
    @GetMapping("/leave/{leaveId}")
    public List<String> getFilesByLeaveId(@PathVariable Long leaveId) {
        return attachmentRepository.findByLeaveApplication_Id(leaveId)
                .stream()
                .map(LeaveAttachment::getFileUrl)
                .collect(Collectors.toList());
    }
}