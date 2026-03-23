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
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final PayslipPdfService pdfService;
    private final LopRecordRepository lopRepository;
    private final LopService lopService;

    public PayslipService(PayslipRepository payslipRepository,
                          PayslipPdfService pdfService,
                          LopRecordRepository lopRepository,
                          LopService lopService
                          ) {
        this.payslipRepository = payslipRepository;
        this.pdfService = pdfService;
        this.lopRepository = lopRepository;
        this.lopService = lopService;
    }

    private BigDecimal safe(BigDecimal v){
        return v == null ? BigDecimal.ZERO : v;
    }

    // CREATE NEW PAYSLIP
    public PayslipResponse createPayslip(CreatePayslipRequest req){

        Optional<Payslip> existing =
                payslipRepository.findByEmployeeIdAndYearAndMonth(
                        req.getEmployeeId(),
                        req.getYear(),
                        req.getMonth());

        if(existing.isPresent() && existing.get().getStatus()!=PayrollStatus.DELETED){
            throw new RuntimeException("Payslip already exists. Use update.");
        }

        Payslip p = existing.orElse(new Payslip());

        fillPayslip(p,req);

        p.setStatus(PayrollStatus.DRAFT);

        return PayslipMapper.toResponse(
                payslipRepository.save(p));
    }

    // UPDATE DRAFT PAYSLIP
    public PayslipResponse updatePayslip(CreatePayslipRequest req){

        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(
                        req.getEmployeeId(),
                        req.getYear(),
                        req.getMonth())
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        if(p.getStatus() == PayrollStatus.GENERATED){
            throw new RuntimeException("Payroll already generated. Delete and recreate.");
        }

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

        calculatePayroll(p);

        Payslip saved = payslipRepository.save(p);

        return PayslipMapper.toResponse(saved);
    }

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

        // ✅ CFO enters this manually
        p.setLop(safe(req.getLop()));

        p.setVariablePay(safe(req.getVariablePay()));

        // 🔥 GET PREVIOUS MONTH LOP DAYS
        int prevMonth = req.getMonth() == 1 ? 12 : req.getMonth() - 1;
        int prevYear  = req.getMonth() == 1 ? req.getYear() - 1 : req.getYear();

        Double lopDays = lopService.getMyMonthlyLopTotal(
                req.getEmployeeId(),
                prevYear,
                prevMonth
        );

        // ✅ Set LOP days from backend
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
                        .add(safe(p.getVariablePay()));  // 🔥 ADDED HERE

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

    public List<PayslipResponse> getEmployeeHistory(Long employeeId){

        return payslipRepository
                .findByEmployeeIdAndStatus(employeeId,PayrollStatus.GENERATED)
                .stream()
                .map(PayslipMapper::toResponse)
                .toList();
    }

    public PayslipResponse getEmployeePayslip(Long employeeId,Integer year,Integer month){

        Payslip p =
                payslipRepository.findByEmployeeIdAndYearAndMonth(
                                employeeId,year,month)
                        .orElseThrow(() -> new RuntimeException("Payslip not found"));

        if(p.getStatus()!=PayrollStatus.GENERATED){
            throw new RuntimeException("Payslip not generated yet");
        }

        return PayslipMapper.toResponse(p);
    }

    public byte[] downloadPayslip(Long employeeId, Integer year, Integer month) {

        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        if (p.getStatus() != PayrollStatus.GENERATED) {
            throw new RuntimeException("Payslip not generated yet");
        }

        ByteArrayInputStream bis = pdfService.generatePdf(p);
        return bis.readAllBytes(); // convert here itself
    }

    public YearlySummaryResponse yearlySummary(Long employeeId,Integer year){

        List<Payslip> payslips =
                payslipRepository.findByEmployeeIdAndStatus(
                        employeeId,
                        PayrollStatus.GENERATED);

        BigDecimal totalBasic = BigDecimal.ZERO;
        BigDecimal totalHra = BigDecimal.ZERO;
        BigDecimal totalConv = BigDecimal.ZERO;
        BigDecimal totalMedical = BigDecimal.ZERO;
        BigDecimal totalOther = BigDecimal.ZERO;

        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalIncentive = BigDecimal.ZERO;
        BigDecimal totalStipend = BigDecimal.ZERO;

        BigDecimal totalPf = BigDecimal.ZERO;
        BigDecimal totalEsi = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalTds = BigDecimal.ZERO;
        BigDecimal totalLop = BigDecimal.ZERO;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for(Payslip p : payslips){

            if(!p.getYear().equals(year)) continue;

            totalBasic = totalBasic.add(safe(p.getBasicSalary()));
            totalHra = totalHra.add(safe(p.getHra()));
            totalConv = totalConv.add(safe(p.getConveyance()));
            totalMedical = totalMedical.add(safe(p.getMedical()));
            totalOther = totalOther.add(safe(p.getOtherAllowance()));

            totalBonus = totalBonus.add(safe(p.getBonus()));
            totalIncentive = totalIncentive.add(safe(p.getIncentive()));
            totalStipend = totalStipend.add(safe(p.getStipend()));

            totalPf = totalPf.add(safe(p.getPf()));
            totalEsi = totalEsi.add(safe(p.getEsi()));
            totalTax = totalTax.add(safe(p.getProfessionalTax()));
            totalTds = totalTds.add(safe(p.getTds()));
            totalLop = totalLop.add(safe(p.getLop()));

            totalGross = totalGross.add(safe(p.getGrossSalary()));
            totalNet = totalNet.add(safe(p.getNetSalary()));
        }

        YearlySummaryResponse res = new YearlySummaryResponse();

        res.setYear(year);

        res.setTotalBasic(totalBasic);
        res.setTotalHra(totalHra);
        res.setTotalConveyance(totalConv);
        res.setTotalMedical(totalMedical);
        res.setTotalOtherAllowance(totalOther);

        res.setTotalBonus(totalBonus);
        res.setTotalIncentive(totalIncentive);
        res.setTotalStipend(totalStipend);

        res.setTotalPf(totalPf);
        res.setTotalEsi(totalEsi);
        res.setTotalProfessionalTax(totalTax);
        res.setTotalTds(totalTds);
        res.setTotalLop(totalLop);

        res.setTotalGrossSalary(totalGross);
        res.setTotalNetSalary(totalNet);

        return res;
    }

    public void deletePayslip(Long employeeId,Integer year,Integer month){

        Payslip p =
                payslipRepository.findByEmployeeIdAndYearAndMonth(
                                employeeId,year,month)
                        .orElseThrow(() -> new RuntimeException("Payslip not found"));

        // mark as deleted instead of blocking
        p.setStatus(PayrollStatus.DELETED);

        payslipRepository.save(p);
    }
    public List<PayslipResponse> getEmployeeHistory(Long employeeId, Integer year){

        List<Payslip> payslips =
                payslipRepository.findByEmployeeIdAndStatus(
                        employeeId,
                        PayrollStatus.GENERATED);

        return payslips.stream()
                .filter(p -> p.getYear().equals(year))
                .map(PayslipMapper::toResponse)
                .toList();
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

            // COPY EVERYTHING
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
        }

        // ❗ IMPORTANT
        p.setLop(BigDecimal.ZERO);

        // ✅ GET PREVIOUS MONTH LOP DAYS
        Double lopDays = lopService.getMyMonthlyLopTotal(employeeId, prevYear, prevMonth);
        p.setLopDays(lopDays != null ? lopDays : 0.0);

        calculatePayroll(p);

        return PayslipMapper.toResponse(p);
    }
}