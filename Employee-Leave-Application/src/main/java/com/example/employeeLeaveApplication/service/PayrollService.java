package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.LopSummaryResponse;
import com.example.employeeLeaveApplication.entity.Payslip;
import com.example.employeeLeaveApplication.enums.PayrollStatus;
import com.example.employeeLeaveApplication.repository.PayslipRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    private final PayslipRepository payslipRepository;
    private final LopService lopService;

    public PayrollService(PayslipRepository payslipRepository,
                          LopService lopService){
        this.payslipRepository = payslipRepository;
        this.lopService = lopService;
    }

    // GENERATE PAYROLL
    public void generatePayroll(Integer year,Integer month){

        List<Payslip> payslips =
                payslipRepository.findByYearAndMonthAndStatusNot(
                        year,month,PayrollStatus.DELETED);

        for(Payslip p : payslips){

            if(p.getStatus()!=PayrollStatus.DRAFT) continue;

            p.setGeneratedDate(LocalDate.now());
            p.setStatus(PayrollStatus.GENERATED);

            payslipRepository.save(p);
        }
    }

    // PREPARE PAYROLL
    public void preparePayroll(Integer year,Integer month){

        int previousMonth = month==1?12:month-1;
        int previousYear = month==1?year-1:year;

        List<Payslip> previous =
                payslipRepository.findByYearAndMonthAndStatusNot(
                        previousYear,previousMonth,PayrollStatus.DELETED);

        for(Payslip prev:previous){

            boolean exists =
                    payslipRepository.existsByEmployeeIdAndYearAndMonth(
                            prev.getEmployeeId(),year,month);

            if(exists) continue;

            Payslip p = new Payslip();

            p.setEmployeeId(prev.getEmployeeId());
            p.setYear(year);
            p.setMonth(month);

            // COPY SALARY STRUCTURE
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

            // RESET LOP AMOUNT
            p.setLop(BigDecimal.ZERO);

            // FETCH LOP DAYS FROM LOP SERVICE
//            Optional<LossOfPayRecord> lop =
//                    lopRepository.findByEmployeeIdAndYearAndMonth(
//                            prev.getEmployeeId(), previousYear, previousMonth);
//
//            if(lop.isPresent()){
//                p.setLopDays(lop.get().getExcessDays());
//            }else{
//                p.setLopDays(0.0);
//            }

            p.setStatus(PayrollStatus.DRAFT);

            payslipRepository.save(p);
        }
    }
}