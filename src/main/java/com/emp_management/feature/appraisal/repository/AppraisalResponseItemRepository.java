package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalResponseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalResponseItemRepository extends JpaRepository<AppraisalResponseItem, Long> {
    List<AppraisalResponseItem> findByResponseId(Long responseId);
    Optional<AppraisalResponseItem> findByResponseIdAndQuestionId(Long responseId, Long questionId);
    void deleteByResponseId(Long responseId);
}