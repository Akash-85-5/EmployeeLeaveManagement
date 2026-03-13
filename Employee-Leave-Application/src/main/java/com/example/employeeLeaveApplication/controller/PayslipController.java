package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.GeneratePayslipRequest;
import com.example.employeeLeaveApplication.entity.Payslip;
import com.example.employeeLeaveApplication.repository.PayslipRepository;
import com.example.employeeLeaveApplication.security.CustomUserDetails;
import com.example.employeeLeaveApplication.service.PayslipPdfService;
import com.example.employeeLeaveApplication.service.PayslipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/payslip")
public class PayslipController {

    private final PayslipService payslipService;
    private final PayslipRepository payslipRepository;
    private final PayslipPdfService payslipPdfService;

    public PayslipController(PayslipService payslipService,
                             PayslipRepository payslipRepository,
                             PayslipPdfService payslipPdfService) {
        this.payslipService = payslipService;
        this.payslipRepository = payslipRepository;
        this.payslipPdfService = payslipPdfService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('HR')")
    public Payslip generatePayslip(
            @RequestBody GeneratePayslipRequest request) {

        return payslipService.generatePayslip(request);
    }


    @GetMapping("/my/{year}/{month}")
    @PreAuthorize("hasRole('HR')")
    public Payslip getMyPayslip(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long employeeId = userDetails.getUser().getId();

        return payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));
    }

    // Get my payslip history
    @GetMapping("/history")

    public List<Payslip> getMyPayslipHistory(Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long employeeId = userDetails.getUser().getId();

        return payslipRepository.findByEmployeeId(employeeId);
    }
    @PutMapping("/update/{id}")
    public Payslip updatePayslip(
            @PathVariable Long id,
            @RequestBody Payslip updated) {

        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        payslip.setBasicSalary(updated.getBasicSalary());
        payslip.setHra(updated.getHra());
        payslip.setConveyance(updated.getConveyance());
        payslip.setMedical(updated.getMedical());
        payslip.setOtherAllowance(updated.getOtherAllowance());
        payslip.setPf(updated.getPf());
        payslip.setProfessionalTax(updated.getProfessionalTax());
        payslip.setEsi(updated.getEsi());
        payslip.setLop(updated.getLop());
        payslip.setNetSalary(updated.getNetSalary());

        return payslipRepository.save(payslip);
    }

    @DeleteMapping("/{id}")
    public String deletePayslip(@PathVariable Long id) {

        payslipRepository.deleteById(id);

        return "Payslip deleted successfully";
    }
    @GetMapping("/download/{employeeId}/{year}/{month}")
    public ResponseEntity<byte[]> downloadPayslip(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {

        Payslip payslip = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        ByteArrayInputStream pdf =
                payslipPdfService.generatePdf(payslip);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=payslip.pdf")
                .body(pdf.readAllBytes());
    }
    @GetMapping("/export/{year}/{month}")
    public ResponseEntity<String> exportPayroll(
            @PathVariable Integer year,
            @PathVariable Integer month) {

        List<Payslip> payslips = payslipRepository.findByYearAndMonth(year, month);

        StringBuilder csv = new StringBuilder();

        csv.append("EmployeeId,Year,Month,Basic,HRA,Conveyance,PF,ProfessionalTax,LOP,NetSalary\n");

        for (Payslip p : payslips) {
            csv.append(p.getEmployeeId()).append(",")
                    .append(p.getYear()).append(",")
                    .append(p.getMonth()).append(",")
                    .append(p.getBasicSalary()).append(",")
                    .append(p.getHra()).append(",")
                    .append(p.getConveyance()).append(",")
                    .append(p.getPf()).append(",")
                    .append(p.getProfessionalTax()).append(",")
                    .append(p.getLop()).append(",")
                    .append(p.getNetSalary()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=payroll_" + year + "_" + month + ".csv")
                .body(csv.toString());
    }

    @GetMapping("/export/range")
    public List<Payslip> exportPayrollRange(
            @RequestParam Integer startYear,
            @RequestParam Integer startMonth,
            @RequestParam Integer endYear,
            @RequestParam Integer endMonth) {

        return payslipRepository.findPayslipsBetweenDates(
                startYear,
                startMonth,
                endYear,
                endMonth
        );
    }
    @DeleteMapping("/payroll/{year}/{month}")
    public String deletePayroll(
            @PathVariable Integer year,
            @PathVariable Integer month) {

        List<Payslip> payslips = payslipRepository.findByYearAndMonth(year, month);

        payslipRepository.deleteAll(payslips);

        return "Payroll deleted successfully for " + month + "/" + year;
    }
    @GetMapping("/employee/{employeeId}/{year}/{month}")
    public Payslip getEmployeePayslip(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {

        return payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));
    }
    @GetMapping("/payroll/{year}/{month}")
    public List<Payslip> getPayrollByMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {

        return payslipRepository.findByYearAndMonth(year, month);
    }
}