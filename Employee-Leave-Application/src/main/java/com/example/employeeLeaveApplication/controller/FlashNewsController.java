package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.FlashNewsRequest;
import com.example.employeeLeaveApplication.entity.FlashNews;
import com.example.employeeLeaveApplication.service.FlashNewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/flash-news")
public class FlashNewsController {

    private final FlashNewsService service;

    public FlashNewsController(FlashNewsService service) {
        this.service = service;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<FlashNews> createFlashNews(@RequestBody FlashNewsRequest request) {
        return ResponseEntity.ok(service.createFlashNews(request));
    }

    // Employees see all flash news
    @GetMapping
     public List<FlashNews> getFlashNews() {
        return service.getActiveFlashNews();
    }
    @GetMapping("/history")
    public List<FlashNews> getFlashNewsHistory() {

        return service.getAllFlashNews();
    }
    @DeleteMapping("/{id}")
    public String deleteFlashNews(@PathVariable Long id) {

        service.deleteFlashNews(id);

        return "Flash news deleted successfully";
    }
}