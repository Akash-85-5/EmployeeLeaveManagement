package com.emp_management.feature.appraisal.service;

import com.emp_management.feature.appraisal.dto.MetricMasterRequest;
import com.emp_management.feature.appraisal.dto.QuestionMasterRequest;
import com.emp_management.feature.appraisal.entity.AppraisalMetricMaster;
import com.emp_management.feature.appraisal.entity.AppraisalQuestionMaster;
import com.emp_management.feature.appraisal.repository.AppraisalMetricMasterRepository;
import com.emp_management.feature.appraisal.repository.AppraisalQuestionMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class AppraisalMasterService {

    private final AppraisalMetricMasterRepository metricRepo;
    private final AppraisalQuestionMasterRepository questionRepo;

    public AppraisalMasterService(AppraisalMetricMasterRepository metricRepo,
                                  AppraisalQuestionMasterRepository questionRepo) {
        this.metricRepo = metricRepo;
        this.questionRepo = questionRepo;
    }

    public AppraisalMetricMaster createMetric(MetricMasterRequest req, String createdBy) {
        if (req.getMinRating() >= req.getMaxRating())
            throw new IllegalArgumentException("minRating must be less than maxRating");
        AppraisalMetricMaster metric = new AppraisalMetricMaster();
        metric.setMetricType(req.getMetricType());
        metric.setMetricDescription(req.getMetricDescription());
        metric.setMinRating(req.getMinRating());
        metric.setMaxRating(req.getMaxRating());
        metric.setEffectiveFrom(req.getEffectiveFrom());
        metric.setEffectiveTo(req.getEffectiveTo());
        metric.setIsActive(req.getIsActive());
        metric.setCreatedBy(createdBy);
        return metricRepo.save(metric);
    }

    public AppraisalMetricMaster updateMetric(Long id, MetricMasterRequest req, String updatedBy) {
        AppraisalMetricMaster metric = metricRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found: " + id));
        metric.setMetricType(req.getMetricType());
        metric.setMetricDescription(req.getMetricDescription());
        metric.setMinRating(req.getMinRating());
        metric.setMaxRating(req.getMaxRating());
        metric.setEffectiveFrom(req.getEffectiveFrom());
        metric.setEffectiveTo(req.getEffectiveTo());
        metric.setIsActive(req.getIsActive());
        metric.setUpdatedBy(updatedBy);
        return metricRepo.save(metric);
    }

    @Transactional(readOnly = true)
    public List<AppraisalMetricMaster> getAllActiveMetrics() {
        return metricRepo.findByIsActiveTrueOrderByMetricType();
    }

    @Transactional(readOnly = true)
    public List<AppraisalMetricMaster> getAllMetrics() {
        return metricRepo.findAll();
    }

    public void toggleMetricActive(Long id, boolean active, String updatedBy) {
        AppraisalMetricMaster metric = metricRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found: " + id));
        metric.setIsActive(active);
        metric.setUpdatedBy(updatedBy);
        metricRepo.save(metric);
    }

    public AppraisalQuestionMaster createQuestion(QuestionMasterRequest req, String createdBy) {
        AppraisalMetricMaster metric = metricRepo.findById(req.getMetricId())
                .orElseThrow(() -> new RuntimeException("Metric not found: " + req.getMetricId()));
        AppraisalQuestionMaster question = new AppraisalQuestionMaster();
        question.setMetric(metric);
        question.setQuestionText(req.getQuestionText());
        question.setDisplayOrder(req.getDisplayOrder());
        question.setIsActive(req.getIsActive());
        question.setCreatedBy(createdBy);
        return questionRepo.save(question);
    }

    public AppraisalQuestionMaster updateQuestion(Long id, QuestionMasterRequest req, String updatedBy) {
        AppraisalQuestionMaster question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found: " + id));
        AppraisalMetricMaster metric = metricRepo.findById(req.getMetricId())
                .orElseThrow(() -> new RuntimeException("Metric not found: " + req.getMetricId()));
        question.setMetric(metric);
        question.setQuestionText(req.getQuestionText());
        question.setDisplayOrder(req.getDisplayOrder());
        question.setIsActive(req.getIsActive());
        question.setUpdatedBy(updatedBy);
        return questionRepo.save(question);
    }

    @Transactional(readOnly = true)
    public List<AppraisalQuestionMaster> getQuestionsByMetric(Long metricId) {
        return questionRepo.findByMetricIdAndIsActiveTrueOrderByDisplayOrder(metricId);
    }

    @Transactional(readOnly = true)
    public List<AppraisalQuestionMaster> getAllActiveQuestions() {
        return questionRepo.findByIsActiveTrueOrderByMetricIdAscDisplayOrderAsc();
    }

    public void toggleQuestionActive(Long id, boolean active, String updatedBy) {
        AppraisalQuestionMaster q = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found: " + id));
        q.setIsActive(active);
        q.setUpdatedBy(updatedBy);
        questionRepo.save(q);
    }
}