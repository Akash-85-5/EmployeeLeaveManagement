package com.example.notificationservice.controller;

import com.example.notificationservice.entity.LeaveAttachment;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.repository.LeaveAttachmentRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final LeaveAttachmentRepository attachmentRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    public FileController(LeaveAttachmentRepository attachmentRepository,
                          LeaveApplicationRepository leaveApplicationRepository) {
        this.attachmentRepository = attachmentRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
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

    private String buildPublicUrl(String filename, HttpServletRequest request) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
            if (!hostname.endsWith(".local")) {
                hostname += ".local";
            }
            int port = request.getServerPort();

            // This converts "web 1.pdf" to "web%201.pdf" for the DB link
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return String.format("http://%s:%d/api/files/download/%s", hostname, port, encodedFilename);
        } catch (Exception e) {
            return "/api/files/download/" + filename;
        }
    }

    @PostMapping("/upload/{leaveId}")
    public ResponseEntity<String> uploadFiles(
            @PathVariable Long leaveId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest request
    ) throws IOException {
        LeaveApplication leaveApplication = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("LeaveApplication not found"));

        if (files == null || files.length == 0) return ResponseEntity.ok("No files uploaded.");

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(uploadDir).resolve(uniqueFileName).normalize();
                file.transferTo(filePath.toFile());

                String fullUrl = buildPublicUrl(uniqueFileName, request);

                LeaveAttachment attachment = new LeaveAttachment();
                attachment.setFileUrl(fullUrl);
                attachment.setLeaveApplication(leaveApplication);
                attachmentRepository.save(attachment);
            }
        }
        return ResponseEntity.ok("Files uploaded successfully!");
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        // Decode %20 back to spaces so Java can find the file on your hard drive
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        Path filePath = Paths.get(uploadDir).resolve(decodedFilename).normalize();

        if (!Files.exists(filePath)) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/leave/{leaveId}")
    public List<String> getFilesByLeaveId(@PathVariable Long leaveId) {
        return attachmentRepository.findByLeaveApplication_Id(leaveId)
                .stream()
                .map(LeaveAttachment::getFileUrl)
                .collect(Collectors.toList());
    }
}