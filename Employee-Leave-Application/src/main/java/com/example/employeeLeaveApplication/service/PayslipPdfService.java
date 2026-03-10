package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Payslip;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class PayslipPdfService {

    public ByteArrayInputStream generatePdf(Payslip payslip) {

        Document document = new Document();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {

            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Employee Payslip"));
            document.add(new Paragraph("Employee ID: " + payslip.getEmployeeId()));
            document.add(new Paragraph("Year: " + payslip.getYear()));
            document.add(new Paragraph("Month: " + payslip.getMonth()));

            document.add(new Paragraph("Basic Salary: " + payslip.getBasicSalary()));
            document.add(new Paragraph("HRA: " + payslip.getHra()));
            document.add(new Paragraph("Transport: " + payslip.getTransportAllowance()));

            document.add(new Paragraph("PF Deduction: " + payslip.getPfDeduction()));
            document.add(new Paragraph("Tax Deduction: " + payslip.getTaxDeduction()));
            document.add(new Paragraph("LOP Deduction: " + payslip.getLopDeduction()));

            document.add(new Paragraph("Net Salary: " + payslip.getNetSalary()));

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
