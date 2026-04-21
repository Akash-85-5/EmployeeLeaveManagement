package com.emp_management.infrastructure.storage;

import com.emp_management.shared.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/v1/documents")
public class DocumentController {

    @Value("${app.document.upload-dir}")
    private String uploadDir;
    @GetMapping("/view")
//    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> viewDocument(@RequestParam String path) {

        // Prevent path traversal attacks (e.g. ../../etc/passwd)
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

    @GetMapping("/download")
//    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@RequestParam String path) {

        if (path.contains("..")) {
            throw new BadRequestException("Invalid document path.");
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Document not found: " + path);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid document path: " + path);
        }
    }

    private String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        return "application/octet-stream";
    }
}