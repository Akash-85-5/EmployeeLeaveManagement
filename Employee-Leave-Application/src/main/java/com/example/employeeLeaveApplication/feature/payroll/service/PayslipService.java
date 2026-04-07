package com.example.employeeLeaveApplication.feature.payroll.service;
import java.util.stream.Collectors;
import com.example.employeeLeaveApplication.feature.employee.dto.ExperiencedPersonalDetailsRequest;
import com.example.employeeLeaveApplication.feature.employee.entity.EmployeePersonalDetails;
import com.example.employeeLeaveApplication.feature.employee.repository.EmployeePersonalDetailsRepository;
import com.example.employeeLeaveApplication.feature.employee.service.EmployeeService;
import com.example.employeeLeaveApplication.feature.payroll.dto.CreatePayslipRequest;
import com.example.employeeLeaveApplication.feature.payroll.dto.MonthlyPayslipResponse;
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
    private final EmployeeService employeeService;
    private final EmployeePersonalDetailsRepository employeePersonalDetailsRepository;

    public PayslipService(PayslipRepository payslipRepository,
                          PayslipPdfService pdfService,
                          LopService lopService,
                          EmployeeService employeeService,
                          EmployeePersonalDetailsRepository employeePersonalDetailsRepository) {
        this.payslipRepository = payslipRepository;
        this.pdfService = pdfService;
        this.lopService = lopService;
        this.employeeService = employeeService;
        this.employeePersonalDetailsRepository = employeePersonalDetailsRepository;
    }

    private BigDecimal safe(BigDecimal v){
        return v == null ? BigDecimal.ZERO : v;
    }

    // CREATE
    public PayslipResponse createPayslip(CreatePayslipRequest req){

        Payslip existing = payslipRepository
                .findByEmployeeIdAndYearAndMonth(
                        req.getEmployeeId(),
                        req.getYear(),
                        req.getMonth())
                .orElse(null);

        if(existing != null && existing.getStatus() != PayrollStatus.DELETED){

            if(existing.getStatus() == PayrollStatus.GENERATED){
                throw new BadRequestException("Payslip already GENERATED for this month");
            }

            throw new BadRequestException("Payslip already exists in DRAFT");
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

        if(p.getStatus() == PayrollStatus.DELETED){
            throw new BadRequestException("Payslip deleted");
        }

        // ✅ Allow both DRAFT and GENERATED
        return PayslipMapper.toResponse(p);
    }

    public byte[] downloadPayslip(Long employeeId, Integer year, Integer month) {

        Payslip p = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        EmployeePersonalDetails emp = employeePersonalDetailsRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee details not found"));

        ByteArrayInputStream bis = pdfService.generatePdf(p, emp);

        return bis.readAllBytes();
    }

    public YearlySummaryResponse yearlySummary(Integer year){

        List<Payslip> payslips = payslipRepository
                .findByYearAndStatusNot(year, PayrollStatus.DELETED);

        YearlySummaryResponse res = new YearlySummaryResponse();
        res.setYear(year);

        BigDecimal totalBasic = BigDecimal.ZERO;
        BigDecimal totalHra = BigDecimal.ZERO;
        BigDecimal totalConveyance = BigDecimal.ZERO;
        BigDecimal totalMedical = BigDecimal.ZERO;
        BigDecimal totalOtherAllowance = BigDecimal.ZERO;

        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalIncentive = BigDecimal.ZERO;
        BigDecimal totalStipend = BigDecimal.ZERO;

        BigDecimal totalPf = BigDecimal.ZERO;
        BigDecimal totalEsi = BigDecimal.ZERO;
        BigDecimal totalProfessionalTax = BigDecimal.ZERO;
        BigDecimal totalTds = BigDecimal.ZERO;
        BigDecimal totalLop = BigDecimal.ZERO;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for(Payslip p : payslips){

            totalBasic = totalBasic.add(safe(p.getBasicSalary()));
            totalHra = totalHra.add(safe(p.getHra()));
            totalConveyance = totalConveyance.add(safe(p.getConveyance()));
            totalMedical = totalMedical.add(safe(p.getMedical()));
            totalOtherAllowance = totalOtherAllowance.add(safe(p.getOtherAllowance()));

            totalBonus = totalBonus.add(safe(p.getBonus()));
            totalIncentive = totalIncentive.add(safe(p.getIncentive()));
            totalStipend = totalStipend.add(safe(p.getStipend()));

            totalPf = totalPf.add(safe(p.getPf()));
            totalEsi = totalEsi.add(safe(p.getEsi()));
            totalProfessionalTax = totalProfessionalTax.add(safe(p.getProfessionalTax()));
            totalTds = totalTds.add(safe(p.getTds()));
            totalLop = totalLop.add(safe(p.getLop()));

            totalGross = totalGross.add(safe(p.getGrossSalary()));
            totalNet = totalNet.add(safe(p.getNetSalary()));
        }

        // ✅ set all values
        res.setTotalBasic(totalBasic);
        res.setTotalHra(totalHra);
        res.setTotalConveyance(totalConveyance);
        res.setTotalMedical(totalMedical);
        res.setTotalOtherAllowance(totalOtherAllowance);

        res.setTotalBonus(totalBonus);
        res.setTotalIncentive(totalIncentive);
        res.setTotalStipend(totalStipend);

        res.setTotalPf(totalPf);
        res.setTotalEsi(totalEsi);
        res.setTotalProfessionalTax(totalProfessionalTax);
        res.setTotalTds(totalTds);
        res.setTotalLop(totalLop);

        res.setTotalGrossSalary(totalGross);
        res.setTotalNetSalary(totalNet);

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
    public YearlySummaryResponse getEmployeeYearlySummary(Long employeeId, Integer year){

        List<Payslip> payslips = payslipRepository
                .findByEmployeeIdAndStatus(employeeId, PayrollStatus.GENERATED)
                .stream()
                .filter(p -> p.getYear().equals(year))
                .toList();

        YearlySummaryResponse res = new YearlySummaryResponse();
        res.setYear(year);

        BigDecimal totalBasic = BigDecimal.ZERO;
        BigDecimal totalHra = BigDecimal.ZERO;
        BigDecimal totalConveyance = BigDecimal.ZERO;
        BigDecimal totalMedical = BigDecimal.ZERO;
        BigDecimal totalOtherAllowance = BigDecimal.ZERO;

        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalIncentive = BigDecimal.ZERO;
        BigDecimal totalStipend = BigDecimal.ZERO;

        BigDecimal totalPf = BigDecimal.ZERO;
        BigDecimal totalEsi = BigDecimal.ZERO;
        BigDecimal totalProfessionalTax = BigDecimal.ZERO;
        BigDecimal totalTds = BigDecimal.ZERO;
        BigDecimal totalLop = BigDecimal.ZERO;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for(Payslip p : payslips){

            totalBasic = totalBasic.add(safe(p.getBasicSalary()));
            totalHra = totalHra.add(safe(p.getHra()));
            totalConveyance = totalConveyance.add(safe(p.getConveyance()));
            totalMedical = totalMedical.add(safe(p.getMedical()));
            totalOtherAllowance = totalOtherAllowance.add(safe(p.getOtherAllowance()));

            totalBonus = totalBonus.add(safe(p.getBonus()));
            totalIncentive = totalIncentive.add(safe(p.getIncentive()));
            totalStipend = totalStipend.add(safe(p.getStipend()));

            totalPf = totalPf.add(safe(p.getPf()));
            totalEsi = totalEsi.add(safe(p.getEsi()));
            totalProfessionalTax = totalProfessionalTax.add(safe(p.getProfessionalTax()));
            totalTds = totalTds.add(safe(p.getTds()));
            totalLop = totalLop.add(safe(p.getLop()));

            totalGross = totalGross.add(safe(p.getGrossSalary()));
            totalNet = totalNet.add(safe(p.getNetSalary()));
        }

        res.setTotalBasic(totalBasic);
        res.setTotalHra(totalHra);
        res.setTotalConveyance(totalConveyance);
        res.setTotalMedical(totalMedical);
        res.setTotalOtherAllowance(totalOtherAllowance);

        res.setTotalBonus(totalBonus);
        res.setTotalIncentive(totalIncentive);
        res.setTotalStipend(totalStipend);

        res.setTotalPf(totalPf);
        res.setTotalEsi(totalEsi);
        res.setTotalProfessionalTax(totalProfessionalTax);
        res.setTotalTds(totalTds);
        res.setTotalLop(totalLop);

        res.setTotalGrossSalary(totalGross);
        res.setTotalNetSalary(totalNet);

        return res;
    }

    public List<MonthlyPayslipResponse> getEmployeeMonthlyBreakdown(Long employeeId, Integer year){

        return payslipRepository
                .findByEmployeeIdAndStatusNot(employeeId, PayrollStatus.DELETED)
                .stream()
                .filter(p -> p.getYear().equals(year))
                .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .map(p -> {
                    MonthlyPayslipResponse res = new MonthlyPayslipResponse();

                    res.setMonth(p.getMonth());
                    res.setYear(p.getYear());

                    res.setBasicSalary(safe(p.getBasicSalary()));
                    res.setHra(safe(p.getHra()));
                    res.setConveyance(safe(p.getConveyance()));
                    res.setMedical(safe(p.getMedical()));
                    res.setOtherAllowance(safe(p.getOtherAllowance()));

                    res.setBonus(safe(p.getBonus()));
                    res.setIncentive(safe(p.getIncentive()));
                    res.setStipend(safe(p.getStipend()));
                    res.setVariablePay(safe(p.getVariablePay()));

                    res.setGrossSalary(safe(p.getGrossSalary()));

                    res.setPf(safe(p.getPf()));
                    res.setEsi(safe(p.getEsi()));
                    res.setProfessionalTax(safe(p.getProfessionalTax()));
                    res.setTds(safe(p.getTds()));

                    res.setLopDays(p.getLopDays());
                    res.setLop(safe(p.getLop()));

                    res.setNetSalary(safe(p.getNetSalary()));

                    res.setGeneratedDate(p.getGeneratedDate());

                    res.setStatus(p.getStatus().name());

                    return res;
                })
                .collect(Collectors.toList());
    }
}