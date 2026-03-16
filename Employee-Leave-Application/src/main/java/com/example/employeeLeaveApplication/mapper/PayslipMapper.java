package com.example.employeeLeaveApplication.mapper;

import com.example.employeeLeaveApplication.dto.PayslipResponse;
import com.example.employeeLeaveApplication.entity.Payslip;

public class PayslipMapper {

    public static PayslipResponse toResponse(Payslip p){

        PayslipResponse r = new PayslipResponse();

        r.setEmployeeId(p.getEmployeeId());
        r.setMonth(p.getMonth());
        r.setYear(p.getYear());

        r.setBasicSalary(p.getBasicSalary());
        r.setHra(p.getHra());
        r.setConveyance(p.getConveyance());
        r.setMedical(p.getMedical());
        r.setOtherAllowance(p.getOtherAllowance());

        r.setBonus(p.getBonus());
        r.setIncentive(p.getIncentive());
        r.setStipend(p.getStipend());

        r.setPf(p.getPf());
        r.setEsi(p.getEsi());
        r.setProfessionalTax(p.getProfessionalTax());
        r.setTds(p.getTds());
        r.setLop(p.getLop());

        r.setGrossSalary(p.getGrossSalary());
        r.setNetSalary(p.getNetSalary());

        return r;
    }
}