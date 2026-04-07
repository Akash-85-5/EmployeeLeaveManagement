package com.example.employeeLeaveApplication.feature.leave.compoff.service;

import com.example.employeeLeaveApplication.feature.leave.compoff.entity.CompOffBalance;
import com.example.employeeLeaveApplication.feature.leave.compoff.repository.CompOffBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompOffBalanceService {

    // ===================== EXISTING =====================
    private final CompOffBalanceRepository balanceRepo;

    // ✅ NEW METHOD
    // Reason: HR/Admin/Manager need to read balance by year
    public CompOffBalance getBalance(Long employeeId, Integer year) {
        return balanceRepo
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);
    }

    // ✅ NEW METHOD
    // Reason: HR/Admin/Manager need full list of balances per employee
    public List<CompOffBalance> getAllByEmployee(Long employeeId) {
        return balanceRepo.findByEmployeeId(employeeId);
    }

    // ✅ NEW METHOD
    // Reason: HR/Admin/Manager need total balance across all years
    public Double getTotalBalance(Long employeeId) {
        Double total = balanceRepo.getTotalAvailableBalance(employeeId);
        return total != null ? total : 0.0;
    }

    // ===================== EXISTING =====================
    @Transactional
    public void addEarned(Long employeeId, BigDecimal days) {
        int year = Year.now().getValue();

        CompOffBalance balance = balanceRepo
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> create(employeeId, year));

        balance.setEarned(balance.getEarned() + days.doubleValue());
        balance.calculateBalance();
        balance.setUpdatedAt(LocalDateTime.now());

        balanceRepo.save(balance);
    }

    // ===================== EXISTING =====================
    @Transactional
    public void restoreUsed(Long employeeId, BigDecimal days) {
        int year = Year.now().getValue();

        CompOffBalance balance = balanceRepo
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() ->
                        new IllegalStateException("Balance record not found"));

        balance.setUsed(balance.getUsed() - days.doubleValue());
        if (balance.getUsed() < 0) balance.setUsed(0.0);

        balance.calculateBalance();
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepo.save(balance);
    }

    // ===================== EXISTING =====================
    @Transactional
    public void addUsed(Long employeeId, BigDecimal days) {
        int year = Year.now().getValue();

        CompOffBalance balance = balanceRepo
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() ->
                        new IllegalStateException("Balance record missing"));

        balance.setUsed(balance.getUsed() + days.doubleValue());
        balance.calculateBalance();
        balance.setUpdatedAt(LocalDateTime.now());

        balanceRepo.save(balance);
    }

    // ===================== EXISTING (UPDATED) =====================
    // Added explicit defaults for earned, used, balance
    // Reason: Prevent null values on first record creation
    private CompOffBalance create(Long employeeId, int year) {
        CompOffBalance b = new CompOffBalance();
        b.setEmployeeId(employeeId);
        b.setYear(year);
        b.setEarned(0.0);   // ✅ NEW LINE - explicit default
        b.setUsed(0.0);     // ✅ NEW LINE - explicit default
        b.setBalance(0.0);  // ✅ NEW LINE - explicit default
        b.setUpdatedAt(LocalDateTime.now());
        return b;
    }
}