package com.example.notificationservice.service;

import com.example.notificationservice.entity.UserSecurityAdmin;
import com.example.notificationservice.repository.UserSecurityAdminRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSecurityAdminService {

    private final UserSecurityAdminRepository repository;

    public UserSecurityAdminService(UserSecurityAdminRepository repository) {
        this.repository = repository;
    }

    public UserSecurityAdmin save(UserSecurityAdmin userSecurityAdmin) {
        return repository.save(userSecurityAdmin);
    }
}
