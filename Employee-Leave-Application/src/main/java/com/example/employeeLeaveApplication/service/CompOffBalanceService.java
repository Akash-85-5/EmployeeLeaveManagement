package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class CompOffBalanceService {

    private final CompOffBalanceRepository balanceRepo;

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

    private CompOffBalance create(Long employeeId, int year) {
        CompOffBalance b = new CompOffBalance();
        b.setEmployeeId(employeeId);
        b.setYear(year);
        b.setUpdatedAt(LocalDateTime.now());
        return b;
    }
}

