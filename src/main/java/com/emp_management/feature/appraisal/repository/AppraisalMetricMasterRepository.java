package com.emp_management.feature.appraisal.repository;

import com.emp_management.feature.appraisal.entity.AppraisalMetricMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.emp_management.shared.enums.MetricType;
import java.util.List;

@Repository
public interface AppraisalMetricMasterRepository extends JpaRepository<AppraisalMetricMaster, Long> {
    List<AppraisalMetricMaster> findByIsActiveTrueOrderByMetricType();
    List<AppraisalMetricMaster> findByMetricTypeAndIsActiveTrue(MetricType metricType);
}