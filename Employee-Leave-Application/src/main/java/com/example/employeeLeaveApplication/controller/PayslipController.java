package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.dto.CreatePayslipRequest;
import com.example.employeeLeaveApplication.dto.PayslipResponse;
import com.example.employeeLeaveApplication.dto.YearlySummaryResponse;
import com.example.employeeLeaveApplication.security.CustomUserDetails;
import com.example.employeeLeaveApplication.service.PayslipPdfService;
import com.example.employeeLeaveApplication.service.PayslipService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/payslip")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService){
        this.payslipService = payslipService;
    }

    @PreAuthorize("hasRole('CFO')")
    @PostMapping("/create")
    public PayslipResponse createPayslip(@RequestBody CreatePayslipRequest request){
        return payslipService.createPayslip(request);
    }

    @PreAuthorize("hasAnyRole('CFO','HR','ADMIN')")
    @GetMapping("/payroll/{year}/{month}")
    public List<PayslipResponse> payroll(
            @PathVariable Integer year,
            @PathVariable Integer month){
        return payslipService.getPayrollByMonth(year,month);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','HR','ADMIN','CFO')")
    @GetMapping("/history/{year}")
    public List<PayslipResponse> history(
            @PathVariable Integer year,
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        Long employeeId = user.getUser().getId();

        return payslipService.getEmployeeHistory(employeeId, year);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN','TEAM_LEADER','HR')")
    @GetMapping("/my/{year}/{month}")
    public PayslipResponse myPayslip(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication auth){

        CustomUserDetails user =
                (CustomUserDetails) auth.getPrincipal();

        return payslipService.getEmployeePayslip(
                user.getUser().getId(),year,month);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN','TEAM_LEADER','HR')")
    @GetMapping("/download/{year}/{month}")
    public ResponseEntity<byte[]> download(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication auth) {

        CustomUserDetails user =
                (CustomUserDetails) auth.getPrincipal();

        byte[] pdf = payslipService.downloadPayslip(
                user.getUser().getId(), year, month);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payslip.pdf")
                .body(pdf);
    }


    @PreAuthorize("hasRole('CFO')")
    @DeleteMapping("/{employeeId}/{year}/{month}")
    public String delete(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month){

        payslipService.deletePayslip(employeeId,year,month);

        return "Payslip deleted successfully";
    }
    @PreAuthorize("hasRole('CFO')")
    @GetMapping("/export/{year}/{month}")
    public ResponseEntity<String> exportPayroll(
            @PathVariable Integer year,
            @PathVariable Integer month){

        List<PayslipResponse> payslips =
                payslipService.getPayrollByMonth(year,month);

        StringBuilder csv = new StringBuilder();

        csv.append("EmployeeId,Year,Month,Basic,HRA,Conveyance,Medical,OtherAllowance,Bonus,Incentive,Stipend,PF,ESI,ProfessionalTax,TDS,LOP,Gross,Net\n");

        for(PayslipResponse p : payslips){

            csv.append(p.getEmployeeId()).append(",")
                    .append(p.getYear()).append(",")
                    .append(p.getMonth()).append(",")
                    .append(p.getBasicSalary()).append(",")
                    .append(p.getHra()).append(",")
                    .append(p.getConveyance()).append(",")
                    .append(p.getMedical()).append(",")
                    .append(p.getOtherAllowance()).append(",")
                    .append(p.getBonus()).append(",")
                    .append(p.getIncentive()).append(",")
                    .append(p.getStipend()).append(",")
                    .append(p.getPf()).append(",")
                    .append(p.getEsi()).append(",")
                    .append(p.getProfessionalTax()).append(",")
                    .append(p.getTds()).append(",")
                    .append(p.getLop()).append(",")
                    .append(p.getGrossSalary()).append(",")
                    .append(p.getNetSalary()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition","attachment; filename=payroll.csv")
                .body(csv.toString());
    }
    @PreAuthorize("hasRole('CFO')")
    @PutMapping("/update")
    public PayslipResponse updatePayslip(@RequestBody CreatePayslipRequest request){

        return payslipService.updatePayslip(request);
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','TEAM_LEADER','MANAGER','HR','ADMIN','CFO')")
    @GetMapping("/summary/{year}")
    public YearlySummaryResponse summary(
            @PathVariable Integer year,
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        Long employeeId = user.getUser().getId();

        return payslipService.yearlySummary(employeeId, year);
    }

    @PreAuthorize("hasRole('CFO')")
    @GetMapping("/employee/{employeeId}/{year}")
    public List<PayslipResponse> getEmployeeYearlyPayslips(
            @PathVariable Long employeeId,
            @PathVariable Integer year){

        return payslipService.getEmployeeHistory(employeeId, year);
    }
    @PreAuthorize("hasRole('CFO')")
    @GetMapping("/prefill")
    public PayslipResponse prefill(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month){

        return payslipService.getPrefillData(employeeId, year, month);
    }
}