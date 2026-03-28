package com.example.employeeLeaveApplication.feature.payroll.service;

import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.payroll.entity.Payslip;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
public class PayslipPdfService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    public ByteArrayInputStream generatePdf(Payslip payslip,
                                            EmployeePersonalDetails emp) {

        // ✅ 1. Set Thymeleaf data
        Context context = new Context();
        context.setVariable("p", payslip);
        context.setVariable("e", emp);

        String html = templateEngine.process("payslip", context);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // ✅ 2. IMPORTANT: Set base path for images (logo fix)
            String baseUrl = new File("src/main/resources/static/").toURI().toString();

            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withHtmlContent(html, baseUrl); // ✅ FIXED
            builder.toStream(out);

            // ✅ Optional but improves layout consistency
            builder.useFastMode();

            builder.run();

        } catch (Exception ex) {
            throw new RuntimeException("PDF generation failed", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}