package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalDefinitionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppraisalDefinitionQuestionRepository extends JpaRepository<AppraisalDefinitionQuestion, Long> {
    List<AppraisalDefinitionQuestion> findByDefinitionIdOrderByDisplayOrder(Long definitionId);
    void deleteByDefinitionId(Long definitionId);
}