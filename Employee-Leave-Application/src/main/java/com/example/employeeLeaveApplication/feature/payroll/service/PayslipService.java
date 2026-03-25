package com.example.employeeLeaveApplication.feature.payroll.service;

import com.example.employeeLeaveApplication.feature.payroll.dto.CreatePayslipRequest;
import com.example.employeeLeaveApplication.feature.payroll.dto.PayslipResponse;
import com.example.employeeLeaveApplication.feature.payroll.dto.YearlySummaryResponse;
import com.example.employeeLeaveApplication.feature.payroll.entity.Payslip;
import com.example.employeeLeaveApplication.feature.leave.lop.service.LopService;
import com.example.employeeLeaveApplication.shared.enums.PayrollStatus;
import com.example.employeeLeaveApplication.feature.payroll.mapper.PayslipMapper;
import com.example.employeeLeaveApplication.feature.leave.lop.repository.LopRecordRepository;
import com.example.employeeLeaveApplication.feature.payroll.repository.PayslipRepository;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import com.example.employeeLeaveApplication.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final PayslipPdfService pdfService;
    private final LopService lopService;

    public PayslipService(PayslipRepository payslipRepository,
                          PayslipPdfService pdfService,
                          LopService lopService) {
        this.payslipRepository = payslipRepository;
        this.pdfService = pdfService;
        this.lopService = lopService;
    }

    private BigDecimal safe(BigDecimal v){
        return v == null ? BigDecimal.ZERO : v;
    }

    // CREATE
    public PayslipResponse createPayslip(CreatePayslipRequest req){

        Optional<Payslip> existing =
                payslipRepository.findByEmployeeIdAndYearAndMonth(
                        req.getEmployeeId(),
                        req.getYear(),
                        req.getMonth());

        if(existing.isPresent() && existing.get().getStatus()!=PayrollStatus.DELETED){
            throw new BadRequestException("Payslip already exists");
        }

        Payslip p = new Payslip();

        fillPayslip(p, req);

        p.setStatus(PayrollStatus.DRAFT);

        return PayslipMapper.toResponse(payslipRepository.save(p));
    }

    // UPDATE
    public PayslipResponse updatePayslip(CreatePayslipRequest req){

        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(
                        req.getEmployeeId(),
                        req.getYear(),
                        req.getMonth())
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        if(p.getStatus() == PayrollStatus.GENERATED){
            throw new BadRequestException("Already generated");
        }

        fillPayslip(p, req);

        return PayslipMapper.toResponse(payslipRepository.save(p));
    }

    // COMMON FILL
    private void fillPayslip(Payslip p, CreatePayslipRequest req){

        p.setEmployeeId(req.getEmployeeId());
        p.setYear(req.getYear());
        p.setMonth(req.getMonth());

        p.setBasicSalary(safe(req.getBasicSalary()));
        p.setHra(safe(req.getHra()));
        p.setConveyance(safe(req.getConveyance()));
        p.setMedical(safe(req.getMedical()));
        p.setOtherAllowance(safe(req.getOtherAllowance()));

        p.setBonus(safe(req.getBonus()));
        p.setIncentive(safe(req.getIncentive()));
        p.setStipend(safe(req.getStipend()));

        p.setPf(safe(req.getPf()));
        p.setEsi(safe(req.getEsi()));
        p.setProfessionalTax(safe(req.getProfessionalTax()));
        p.setTds(safe(req.getTds()));

        p.setLop(safe(req.getLop()));
        p.setVariablePay(safe(req.getVariablePay()));

        int prevMonth = req.getMonth() == 1 ? 12 : req.getMonth() - 1;
        int prevYear  = req.getMonth() == 1 ? req.getYear() - 1 : req.getYear();

        Double lopDays = lopService.getMyMonthlyLopTotal(
                req.getEmployeeId(), prevYear, prevMonth);

        p.setLopDays(lopDays != null ? lopDays : 0.0);

        calculatePayroll(p);
    }

    private void calculatePayroll(Payslip p){

        BigDecimal gross =
                safe(p.getBasicSalary())
                        .add(safe(p.getHra()))
                        .add(safe(p.getConveyance()))
                        .add(safe(p.getMedical()))
                        .add(safe(p.getOtherAllowance()))
                        .add(safe(p.getBonus()))
                        .add(safe(p.getIncentive()))
                        .add(safe(p.getStipend()));

        BigDecimal deductions =
                safe(p.getPf())
                        .add(safe(p.getEsi()))
                        .add(safe(p.getProfessionalTax()))
                        .add(safe(p.getTds()))
                        .add(safe(p.getLop()))
                        .add(safe(p.getVariablePay()));

        p.setGrossSalary(gross);
        p.setNetSalary(gross.subtract(deductions));
    }

    public List<PayslipResponse> getPayrollByMonth(Integer year,Integer month){
        return payslipRepository
                .findByYearAndMonthAndStatusNot(year,month,PayrollStatus.DELETED)
                .stream()
                .map(PayslipMapper::toResponse)
                .toList();
    }

    public List<PayslipResponse> getEmployeeHistory(Long employeeId, Integer year){
        return payslipRepository
                .findByEmployeeIdAndStatus(employeeId, PayrollStatus.GENERATED)
                .stream()
                .filter(p -> p.getYear().equals(year))
                .map(PayslipMapper::toResponse)
                .toList();
    }

    public PayslipResponse getEmployeePayslip(Long employeeId,Integer year,Integer month){
        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId,year,month)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        if(p.getStatus()!=PayrollStatus.GENERATED){
            throw new BadRequestException("Not generated");
        }

        return PayslipMapper.toResponse(p);
    }

    public byte[] downloadPayslip(Long employeeId, Integer year, Integer month) {

        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        ByteArrayInputStream bis = pdfService.generatePdf(p);
        return bis.readAllBytes();
    }

    public YearlySummaryResponse yearlySummary(Long employeeId,Integer year){
        // simplified
        YearlySummaryResponse res = new YearlySummaryResponse();
        res.setYear(year);
        return res;
    }

    public void deletePayslip(Long employeeId,Integer year,Integer month){
        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId,year,month)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        p.setStatus(PayrollStatus.DELETED);
        payslipRepository.save(p);
    }

    public PayslipResponse getPrefillData(Long employeeId, Integer year, Integer month){

        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;

        Optional<Payslip> prevPayslip =
                payslipRepository.findByEmployeeIdAndYearAndMonth(
                        employeeId, prevYear, prevMonth);

        Payslip p = new Payslip();

        p.setEmployeeId(employeeId);
        p.setYear(year);
        p.setMonth(month);

        if(prevPayslip.isPresent()){
            Payslip prev = prevPayslip.get();

            // ✅ COPY ALL FIELDS
            p.setBasicSalary(prev.getBasicSalary());
            p.setHra(prev.getHra());
            p.setConveyance(prev.getConveyance());
            p.setMedical(prev.getMedical());
            p.setOtherAllowance(prev.getOtherAllowance());

            p.setBonus(prev.getBonus());
            p.setIncentive(prev.getIncentive());
            p.setStipend(prev.getStipend());

            p.setPf(prev.getPf());
            p.setEsi(prev.getEsi());
            p.setProfessionalTax(prev.getProfessionalTax());
            p.setTds(prev.getTds());

            p.setVariablePay(prev.getVariablePay());
        }

        // ❗ RESET THESE EVERY MONTH
        p.setLop(BigDecimal.ZERO);

        Double lopDays = lopService.getMyMonthlyLopTotal(employeeId, prevYear, prevMonth);
        p.setLopDays(lopDays != null ? lopDays : 0.0);

        calculatePayroll(p);

        return PayslipMapper.toResponse(p);
    }
}