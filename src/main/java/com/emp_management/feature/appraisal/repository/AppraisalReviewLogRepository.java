package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppraisalReviewLogRepository extends JpaRepository<AppraisalReviewLog, Long> {
    List<AppraisalReviewLog> findByResponseIdOrderByActedAtAsc(Long responseId);
    List<AppraisalReviewLog> findByActorId(String actorId);
}