package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalResponse;
import com.emp_management.shared.enums.AppraisalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalResponseRepository extends JpaRepository<AppraisalResponse, Long> {
    Optional<AppraisalResponse> findByDefinitionIdAndEmployeeId(Long definitionId, String employeeId);
    List<AppraisalResponse> findByEmployeeId(String employeeId);
    List<AppraisalResponse> findByL1ReviewerIdAndStatus(String l1ReviewerId, AppraisalStatus status);
    List<AppraisalResponse> findByL2ReviewerIdAndStatus(String l2ReviewerId, AppraisalStatus status);

    @Query("""
        SELECT r FROM AppraisalResponse r
        JOIN r.definition d
        WHERE r.status = 'DRAFT'
          AND d.autoSubmitEnabled = true
          AND d.closeDate < :today
    """)
    List<AppraisalResponse> findDraftsPastCloseDate(@Param("today") LocalDate today);

    List<AppraisalResponse> findByDefinitionId(Long definitionId);
}