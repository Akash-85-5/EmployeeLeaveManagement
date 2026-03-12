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

    // CREATE SALARY STRUCTURE
    @PostMapping
    public SalaryStructure create(@RequestBody SalaryStructure structure) {

        if (structure.getRole() == null) {
            throw new RuntimeException("Role cannot be null");
        }

        if (repository.existsByRole(structure.getRole())) {
            throw new RuntimeException(
                    "Salary structure already exists for role: " + structure.getRole()
            );
        }

        return repository.save(structure);
    }

    // GET SALARY STRUCTURE BY ROLE
    @GetMapping("/{role}")
    public SalaryStructure getByRole(@PathVariable Role role) {

        return repository.findByRole(role)
                .orElseThrow(() ->
                        new RuntimeException("Salary structure not found for role: " + role)
                );
    }
    @PutMapping("/{role}")
    public SalaryStructure updateStructure(
            @PathVariable Role role,
            @RequestBody SalaryStructure request) {

        SalaryStructure existing = repository.findByRole(role)
                .orElseThrow(() -> new RuntimeException("Structure not found"));

        existing.setHraAmount(request.getHraAmount());
        existing.setPfPercent(request.getPfPercent());
        existing.setTaxPercent(request.getTaxPercent());
        existing.setTransportAllowance(request.getTransportAllowance());

        return repository.save(existing);
    }
}