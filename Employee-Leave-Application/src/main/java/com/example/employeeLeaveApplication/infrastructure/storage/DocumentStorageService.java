package com.example.employeeLeaveApplication.infrastructure.storage;

import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    // Configured in application.properties:
    // app.document.upload-dir=uploads/profile-documents
    @Value("${app.document.upload-dir:uploads/profile-documents}")
    private String uploadDir;

    /**
     * Saves a file to: {uploadDir}/{subFolder}/{employeeId}-{uuid}.{ext}
     * Returns the saved file path string (stored in DB).
     *
     * @param file      the uploaded file
     * @param subFolder e.g. "aadhaar", "tc", "offer-letter"
     * @param employeeId used to prefix the filename
     */
    public String save(MultipartFile file, String subFolder, Long employeeId) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("File is required for: " + subFolder);

        if (file.getSize() > MAX_SIZE)
            throw new BadRequestException("File too large (max 5MB): " + subFolder);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType))
            throw new BadRequestException("Only PDF, JPG, PNG allowed for: " + subFolder);

        String ext = getExtension(file.getOriginalFilename());
        String fileName = employeeId + "-" + UUID.randomUUID() + "." + ext;

        try {
            Path dir = Paths.get(uploadDir, subFolder);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Return relative path stored in DB
            return subFolder + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a previously saved file by its stored path.
     * Called when employee resubmits after rejection.
     * Silent on failure — non-critical cleanup.
     */
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;
        try {
            Path filePath = Paths.get(uploadDir, storedPath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Warning: could not delete old document: " + storedPath);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}