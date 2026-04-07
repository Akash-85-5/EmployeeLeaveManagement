package com.example.employeeLeaveApplication.feature.separation.repository;

import com.example.employeeLeaveApplication.feature.separation.entity.Separation;
import com.example.employeeLeaveApplication.shared.enums.SeparationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeparationRepository extends JpaRepository<Separation, Long> {

    boolean existsByEmployeeIdAndStatus(Long employeeId,
                                        SeparationStatus status);

    boolean existsByEmployeeIdAndStatusIn(Long employeeId,
                                          List<SeparationStatus> statuses);

    Optional<Separation> findByEmployeeIdAndStatus(Long employeeId,
                                                   SeparationStatus status);
}