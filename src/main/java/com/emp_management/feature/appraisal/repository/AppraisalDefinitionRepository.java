package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalDefinitionRepository extends JpaRepository<AppraisalDefinition, Long> {
    List<AppraisalDefinition> findByIsPublishedTrue();
    Optional<AppraisalDefinition> findByAppraisalYear(String appraisalYear);
    List<AppraisalDefinition> findByDefaultL1ReviewerIdOrDefaultL2ReviewerId(
            String l1ReviewerId, String l2ReviewerId);
}