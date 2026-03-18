package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.FlashNewsRequest;
import com.example.employeeLeaveApplication.entity.FlashNews;
import com.example.employeeLeaveApplication.repository.FlashNewsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashNewsService {

    private final FlashNewsRepository repository;

    public FlashNewsService(FlashNewsRepository repository) {
        this.repository = repository;
    }
    public List<FlashNews> getAllFlashNews() {
        return repository.findAll(
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );
    }

    public FlashNews createFlashNews(FlashNewsRequest request) {

        FlashNews news = new FlashNews();
        news.setMessage(request.getMessage());
        news.setActive(true);
        news.setPriority(request.getPriority());
        news.setExpiryDate(
                LocalDateTime.now().plusDays(request.getDays())
        );

        return repository.save(news);
    }

    public List<FlashNews> getActiveFlashNews() {

        return repository
                .findByActiveTrueAndExpiryDateAfterOrderByCreatedAtDesc(
                        LocalDateTime.now());
    }
    public void deleteFlashNews(Long id) {

        FlashNews news = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flash news not found"));

        repository.delete(news);
    }

}