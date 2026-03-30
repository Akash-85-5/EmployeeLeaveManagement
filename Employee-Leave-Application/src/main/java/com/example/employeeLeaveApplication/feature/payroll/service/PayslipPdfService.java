package com.example.employeeLeaveApplication.feature.payroll.service;

import com.example.employeeLeaveApplication.feature.employee.entity.Employee;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.feature.payroll.dto.PayslipPdfData;
import com.example.employeeLeaveApplication.feature.payroll.entity.Payslip;
import com.example.employeeLeaveApplication.shared.exceptions.ResourceNotFoundException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
public class PayslipPdfService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Called by PayslipService.downloadPayslip — signature unchanged
    // ─────────────────────────────────────────────────────────────────────────
    public ByteArrayInputStream generatePdf(Payslip payslip, EmployeePersonalDetails emp) {

        // Fetch Employee (name, joiningDate) using the employeeId stored on emp
        Employee employee = employeeRepository.findById(emp.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Build the flat DTO — only this goes to the template
        PayslipPdfData data = buildPdfData(payslip, emp, employee);

        Context context = new Context();
        context.setVariable("d", data);
        context.setVariable("logoBase64", loadLogoAsBase64());

        String html = templateEngine.process("payslip", context);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUrl = new ClassPathResource("static/").getURL().toExternalForm();            builder.withHtmlContent(html, baseUrl);
            builder.toStream(out);
            builder.useFastMode();
            builder.run();
        } catch (Exception ex) {
            throw new RuntimeException("PDF generation failed", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build the flat DTO from three sources:
    //   payslip  → all salary figures
    //   emp      → bank details, aadhaar, PF/UAN, designation
    //   employee → name, joiningDate
    // ─────────────────────────────────────────────────────────────────────────
    private PayslipPdfData buildPdfData(Payslip p,
                                        EmployeePersonalDetails emp,
                                        Employee employee) {
        PayslipPdfData d = new PayslipPdfData();

        // ── Employee details ──────────────────────────────────────
        // Format numeric id as "WENXT001", "WENXT042", etc.
        d.setEmployeeCode("WENXT" + String.format("%03d", emp.getEmployeeId()));
        d.setEmployeeName(employee.getName());
        d.setDesignation(emp.getDesignation());
        d.setJoiningDate(formatDate(employee.getJoiningDate()));
        d.setAadharNumber(emp.getAadharNumber());
        d.setUanNumber(emp.getUnaNumber());
        d.setPfNumber(emp.getPfNumber());
        d.setAccountNumber(emp.getAccountNumber());
        d.setBankName(emp.getBankName());

        // ── Period ────────────────────────────────────────────────
        d.setMonthYear(buildMonthYear(p.getMonth(), p.getYear()));
        d.setWorkingDays(daysInMonth(p.getMonth(), p.getYear()));
        d.setLopDays(p.getLopDays() != null ? p.getLopDays() : 0.0);

        // ── Income ────────────────────────────────────────────────
        d.setBasicSalary(safe(p.getBasicSalary()));
        d.setHra(safe(p.getHra()));
        d.setConveyance(safe(p.getConveyance()));
        d.setMedical(safe(p.getMedical()));
        d.setOtherAllowance(safe(p.getOtherAllowance()));
        d.setBonus(safe(p.getBonus()));
        d.setGrossSalary(safe(p.getGrossSalary()));

        // ── Deductions ────────────────────────────────────────────
        d.setPf(safe(p.getPf()));
        d.setTds(safe(p.getTds()));
        d.setProfessionalTax(safe(p.getProfessionalTax()));
        d.setVariablePay(safe(p.getVariablePay()));
        d.setEsi(safe(p.getEsi()));
        d.setLop(safe(p.getLop()));
        d.setTotalDeduction(computeTotalDeduction(p));

        // ── Net ───────────────────────────────────────────────────
        d.setNetSalary(safe(p.getNetSalary()));
        d.setNetSalaryInWords(toWords(p.getNetSalary()));

        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal computeTotalDeduction(Payslip p) {
        return safe(p.getPf())
                .add(safe(p.getTds()))
                .add(safe(p.getProfessionalTax()))
                .add(safe(p.getVariablePay()))
                .add(safe(p.getEsi()))
                .add(safe(p.getLop()));
    }

    /** Returns e.g. "Feb 2026" */
    private String buildMonthYear(Integer month, Integer year) {
        if (month == null || year == null) return "";
        String[] months = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };
        return months[month - 1] + " " + year;
    }

    /** Returns actual calendar days in that month, e.g. 28 for Feb 2026 */
    private int daysInMonth(Integer month, Integer year) {
        if (month == null || year == null) return 30;
        return YearMonth.of(year, month).lengthOfMonth();
    }

    /** Returns e.g. "03 October 2025" */
    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH));
    }

    /** Loads wenxt-logo.png from src/main/resources/static/images/ as a base64 data URI */
    private String loadLogoAsBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/wenxt-logo.png");
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            return ""; // logo missing — PDF still generates, just without logo
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Number → Indian-English words
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String toWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Rupees Zero Only";
        }
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        return "Rupees " + convertToWords(value).trim() + " Only";
    }

    private static String convertToWords(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();

        if (n >= 10_00_000) {
            sb.append(convertToWords(n / 10_00_000)).append(" Lakh ");
            n %= 10_00_000;
        }
        if (n >= 1_00_000) {
            sb.append(convertToWords(n / 1_00_000)).append(" Lakh ");
            n %= 1_00_000;
        }
        if (n >= 1000) {
            sb.append(convertToWords(n / 1000)).append(" Thousand ");
            n %= 1000;
        }
        if (n >= 100) {
            sb.append(ONES[(int) (n / 100)]).append(" Hundred ");
            n %= 100;
        }
        if (n >= 20) {
            sb.append(TENS[(int) (n / 10)]);
            if (n % 10 != 0) sb.append(" ").append(ONES[(int) (n % 10)]);
            sb.append(" ");
        } else if (n > 0) {
            sb.append(ONES[(int) n]).append(" ");
        }

        return sb.toString();
    }
}