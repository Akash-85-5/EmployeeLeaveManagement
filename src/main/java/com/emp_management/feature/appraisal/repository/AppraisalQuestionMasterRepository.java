package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalQuestionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppraisalQuestionMasterRepository extends JpaRepository<AppraisalQuestionMaster, Long> {
    List<AppraisalQuestionMaster> findByMetricIdAndIsActiveTrueOrderByDisplayOrder(Long metricId);
    List<AppraisalQuestionMaster> findByIsActiveTrueOrderByMetricIdAscDisplayOrderAsc();
}