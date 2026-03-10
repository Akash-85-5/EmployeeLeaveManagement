package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.SalaryStructure;
import com.example.employeeLeaveApplication.repository.SalaryStructureRepository;
import com.example.employeeLeaveApplication.enums.Role;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary-structure")
public class SalaryStructureController {

    private final SalaryStructureRepository repository;

    public SalaryStructureController(SalaryStructureRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public SalaryStructure create(@RequestBody SalaryStructure structure) {
        return repository.save(structure);
    }

    @GetMapping("/{role}")
    public SalaryStructure getByRole(@PathVariable Role role) {
        return repository.findByRole(role)
                .orElseThrow(() -> new RuntimeException("Structure not found"));
    }
}