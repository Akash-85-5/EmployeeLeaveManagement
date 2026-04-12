package com.emp_management.feature.payroll.controller;

import com.emp_management.feature.payroll.dto.CreatePayslipRequest;
import com.emp_management.feature.payroll.dto.MonthlyPayslipResponse;
import com.emp_management.feature.payroll.dto.PayslipResponse;
import com.emp_management.feature.payroll.dto.YearlySummaryResponse;
import com.emp_management.feature.payroll.service.PayslipService;
import com.emp_management.security.CustomUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payslip")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService){
        this.payslipService = payslipService;
    }

    // ───────────── CFO ACTIONS ─────────────

    @PostMapping("/create")
    public PayslipResponse createPayslip(@RequestBody CreatePayslipRequest request){
        return payslipService.createPayslip(request);
    }

    @PutMapping("/update")
    public PayslipResponse updatePayslip(@RequestBody CreatePayslipRequest request){
        return payslipService.updatePayslip(request);
    }

    @DeleteMapping("/{employeeId}/{year}/{month}")
    public String delete(
            @PathVariable String employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month){

        payslipService.deletePayslip(employeeId,year,month);
        return "Payslip deleted successfully";
    }

    @GetMapping("/prefill")
    public PayslipResponse prefill(
            @RequestParam String employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month){

        return payslipService.getPrefillData(employeeId, year, month);
    }

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

    // ───────────── MANAGEMENT VIEW ─────────────

    @GetMapping("/payroll/{year}/{month}")
    public List<PayslipResponse> payroll(
            @PathVariable Integer year,
            @PathVariable Integer month){
        return payslipService.getPayrollByMonth(year,month);
    }

    @GetMapping("/employee/{employeeId}/{year}")
    public YearlySummaryResponse getEmployeeAnnualSummary(
            @PathVariable String employeeId,
            @PathVariable Integer year) {

        return payslipService.getEmployeeYearlySummary(employeeId, year);
    }

    // ───────────── EMPLOYEE ACTIONS ─────────────

    @GetMapping("/history/{year}")
    public List<PayslipResponse> history(
            @PathVariable Integer year,
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        String employeeId = user.getUser().getEmployee().getEmpId();

        return payslipService.getEmployeeHistory(employeeId, year);
    }

    @GetMapping("/my/{year}/{month}")
    public PayslipResponse myPayslip(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails user){

        return payslipService.getEmployeePayslip(
                user.getEmployeeId(), year, month);
    }

    @GetMapping("/download/{year}/{month}")
    public ResponseEntity<byte[]> download(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @AuthenticationPrincipal CustomUserDetails user) {

        byte[] pdf = payslipService.downloadPayslip(
                user.getEmployeeId(), year, month);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payslip.pdf")
                .body(pdf);
    }

    @GetMapping("/summary/{year}")
    public YearlySummaryResponse summary(@PathVariable Integer year) {

        return payslipService.yearlySummary(year); // ✅ matches service
    }
    @GetMapping("/employee/{employeeId}/{year}/{month}")
    public PayslipResponse getEmployeePayslipByMonth(
            @PathVariable String employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {

        return payslipService.getEmployeePayslip(employeeId, year, month);
    }
    @GetMapping("/employee/{employeeId}/{year}/monthly")
    public List<MonthlyPayslipResponse> getEmployeeMonthlyBreakdown(
            @PathVariable String employeeId,
            @PathVariable Integer year){

        return payslipService.getEmployeeMonthlyBreakdown(employeeId, year);
    }
}
