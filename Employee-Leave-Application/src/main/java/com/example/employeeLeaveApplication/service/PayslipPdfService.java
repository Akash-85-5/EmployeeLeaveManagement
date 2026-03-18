package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.Payslip;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
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
            document.add(new Paragraph("Conveyance: " + payslip.getConveyance()));
            document.add(new Paragraph("Medical: " + payslip.getMedical()));
            document.add(new Paragraph("Other Allowance: " + payslip.getOtherAllowance()));

            document.add(new Paragraph("PF Deduction: " + payslip.getPf()));
            document.add(new Paragraph("Professional Tax: " + payslip.getProfessionalTax()));
            document.add(new Paragraph("ESI Deduction: " + payslip.getEsi()));
            document.add(new Paragraph("LOP Deduction: " + payslip.getLop()));
            document.add(new Paragraph("Variable Pay Deduction: " + payslip.getVariablePay()));

            document.add(new Paragraph("Net Salary: " + payslip.getNetSalary()));

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}