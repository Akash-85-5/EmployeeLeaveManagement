package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.SalaryStructure;
import com.example.employeeLeaveApplication.repository.SalaryStructureRepository;
import com.example.employeeLeaveApplication.enums.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        if (structure.getHra() == null ||
                structure.getConveyance() == null ||
                structure.getMedical() == null ||
                structure.getOtherAllowance() == null ||
                structure.getPfPercent() == null ||
                structure.getProfessionalTax() == null ||
                structure.getEsiPercent() == null) {

            throw new RuntimeException("Salary structure fields cannot be null");
        }

        if (structure.getHra().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getConveyance().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getMedical().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getOtherAllowance().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getPfPercent().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getProfessionalTax().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                structure.getEsiPercent().compareTo(java.math.BigDecimal.ZERO) < 0) {

            throw new RuntimeException("Salary structure values cannot be negative");
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

        existing.setHra(request.getHra());
        existing.setConveyance(request.getConveyance());
        existing.setMedical(request.getMedical());
        existing.setOtherAllowance(request.getOtherAllowance());
        existing.setPfPercent(request.getPfPercent());
        existing.setProfessionalTax(request.getProfessionalTax());
        existing.setEsiPercent(request.getEsiPercent());


        return repository.save(existing);
    }
    @DeleteMapping("/{role}")
    public String deleteSalaryStructure(@PathVariable Role role) {

        SalaryStructure structure = repository.findByRole(role)
                .orElseThrow(() -> new RuntimeException("Salary structure not found"));

        repository.delete(structure);

        return "Salary structure deleted successfully";
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/all")
    public List<SalaryStructure> getAllSalaryStructures() {
        return repository.findAll();
    }
}
