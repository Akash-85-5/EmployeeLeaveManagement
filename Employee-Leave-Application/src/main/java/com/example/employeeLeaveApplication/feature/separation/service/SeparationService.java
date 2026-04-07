package com.example.employeeLeaveApplication.feature.separation.service;

import com.example.employeeLeaveApplication.feature.separation.entity.Separation;
import com.example.employeeLeaveApplication.feature.separation.repository.SeparationRepository;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import com.example.employeeLeaveApplication.shared.enums.SeparationType;
import com.example.employeeLeaveApplication.shared.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SeparationService {

    private final SeparationRepository separationRepository;

    public SeparationService(SeparationRepository separationRepository) {
        this.separationRepository = separationRepository;
    }

    // ─────────────────────────────────────────────
    // APPLY SEPARATION
    // ─────────────────────────────────────────────
    @Transactional
    public Separation applySeparation(Long employeeId,
                                      SeparationType type,
                                      String reason) {

        // Prevent multiple active separations
        if (separationRepository.existsByEmployeeIdAndStatusIn(
                employeeId,
                java.util.List.of(
                        SeparationStatus.PENDING_MANAGER,
                        SeparationStatus.PENDING_HR,
                        SeparationStatus.PENDING_CEO,
                        SeparationStatus.APPROVED,
                        SeparationStatus.NOTICE_PERIOD
                ))) {
            throw new BadRequestException("Employee already has an active separation process.");
        }

        Separation separation = new Separation();
        separation.setEmployeeId(employeeId);
        separation.setType(type);
        separation.setReason(reason);

        // Type-based initial status
        switch (type) {

            case RESIGNATION -> separation.setStatus(SeparationStatus.PENDING_MANAGER);

            case TERMINATION, ABSCONDING -> separation.setStatus(SeparationStatus.PENDING_HR);

            case DEATH_IN_SERVICE -> {
                separation.setStatus(SeparationStatus.RELIEVED);
                separation.setActualRelievingDate(LocalDate.now());
            }

            default -> throw new BadRequestException("Unsupported separation type.");
        }

        return separationRepository.save(separation);
    }

    // ─────────────────────────────────────────────
    // MANAGER APPROVAL
    // ─────────────────────────────────────────────
    @Transactional
    public Separation approveByManager(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.PENDING_MANAGER) {
            throw new BadRequestException("Separation not in Manager approval stage.");
        }

        sep.setStatus(SeparationStatus.PENDING_HR);
        sep.setManagerApprovedAt(LocalDateTime.now());

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // HR APPROVAL
    // ─────────────────────────────────────────────
    @Transactional
    public Separation approveByHR(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.PENDING_HR) {
            throw new BadRequestException("Separation not in HR approval stage.");
        }

        sep.setStatus(SeparationStatus.PENDING_CEO);
        sep.setHrApprovedAt(LocalDateTime.now());

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // CEO APPROVAL
    // ─────────────────────────────────────────────
    @Transactional
    public Separation approveByCEO(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.PENDING_CEO) {
            throw new BadRequestException("Separation not in CEO approval stage.");
        }

        sep.setStatus(SeparationStatus.APPROVED);
        sep.setCeoApprovedAt(LocalDateTime.now());

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // START NOTICE PERIOD (ONLY FOR RESIGNATION)
    // ─────────────────────────────────────────────
    @Transactional
    public Separation startNoticePeriod(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.APPROVED) {
            throw new BadRequestException("Separation not approved yet.");
        }

        if (sep.getType() != SeparationType.RESIGNATION) {
            throw new BadRequestException("Notice period applies only for resignation.");
        }

        sep.setNoticeStartDate(LocalDate.now());
        sep.setNoticeEndDate(LocalDate.now().plusMonths(3));
        sep.setStatus(SeparationStatus.NOTICE_PERIOD);

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // EXTEND NOTICE (FROM LEAVE MODULE)
    // ─────────────────────────────────────────────
    @Transactional
    public void extendNoticePeriod(Long employeeId, int days) {

        Separation sep = separationRepository
                .findByEmployeeIdAndStatus(employeeId, SeparationStatus.NOTICE_PERIOD)
                .orElseThrow(() -> new BadRequestException("No active notice period."));

        sep.setNoticeEndDate(sep.getNoticeEndDate().plusDays(days));

        separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // CHECK IF IN NOTICE
    // ─────────────────────────────────────────────
    public boolean isInNoticePeriod(Long employeeId) {
        return separationRepository
                .existsByEmployeeIdAndStatus(employeeId, SeparationStatus.NOTICE_PERIOD);
    }

    // ─────────────────────────────────────────────
    // COMPLETE NOTICE
    // ─────────────────────────────────────────────
    @Transactional
    public Separation completeNotice(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.NOTICE_PERIOD) {
            throw new BadRequestException("Employee is not in notice period.");
        }

        if (LocalDate.now().isBefore(sep.getNoticeEndDate())) {
            throw new BadRequestException("Notice period not completed yet.");
        }

        sep.setStatus(SeparationStatus.NOTICE_COMPLETED);

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // MARK RELIEVED (FINAL STEP)
    // ─────────────────────────────────────────────
    @Transactional
    public Separation markRelieved(Long separationId) {

        Separation sep = getSeparation(separationId);

        if (sep.getStatus() != SeparationStatus.EXIT_CHECKLIST_DONE
                && sep.getStatus() != SeparationStatus.NOTICE_COMPLETED) {
            throw new BadRequestException("Exit process not completed yet.");
        }

        sep.setStatus(SeparationStatus.RELIEVED);
        sep.setActualRelievingDate(LocalDate.now());

        return separationRepository.save(sep);
    }

    // ─────────────────────────────────────────────
    // COMMON FETCH
    // ─────────────────────────────────────────────
    private Separation getSeparation(Long id) {
        return separationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Separation not found."));
    }
}