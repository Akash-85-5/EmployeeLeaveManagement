package com.emp_management.feature.appraisal.service;

import com.emp_management.feature.appraisal.dto.AppraisalFormSaveRequest;
import com.emp_management.feature.appraisal.dto.AppraisalFormResponse;
import com.emp_management.feature.appraisal.entity.*;
import com.emp_management.shared.enums.AppraisalStatus;
import com.emp_management.feature.appraisal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppraisalFormService {

    private final AppraisalDefinitionRepository definitionRepo;
    private final AppraisalDefinitionQuestionRepository defQuestionRepo;
    private final AppraisalResponseRepository responseRepo;
    private final AppraisalResponseItemRepository responseItemRepo;
    private final AppraisalQuestionMasterRepository questionMasterRepo;

    public AppraisalFormService(AppraisalDefinitionRepository definitionRepo,
                                AppraisalDefinitionQuestionRepository defQuestionRepo,
                                AppraisalResponseRepository responseRepo,
                                AppraisalResponseItemRepository responseItemRepo,
                                AppraisalQuestionMasterRepository questionMasterRepo) {
        this.definitionRepo = definitionRepo;
        this.defQuestionRepo = defQuestionRepo;
        this.responseRepo = responseRepo;
        this.responseItemRepo = responseItemRepo;
        this.questionMasterRepo = questionMasterRepo;
    }

    @Transactional
    public AppraisalFormResponse getOrInitForm(Long definitionId, String employeeId,
                                               String l1ReviewerId, String l2ReviewerId) {
        AppraisalDefinition def = definitionRepo.findById(definitionId)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + definitionId));
        if (!def.getIsPublished()) {
            throw new IllegalStateException("Appraisal form is not published yet");
        }
        AppraisalResponse response = responseRepo
                .findByDefinitionIdAndEmployeeId(definitionId, employeeId)
                .orElseGet(() -> initResponse(def, employeeId, l1ReviewerId, l2ReviewerId));
        List<AppraisalResponseItem> items = responseItemRepo.findByResponseId(response.getId());
        List<AppraisalDefinitionQuestion> defQuestions =
                defQuestionRepo.findByDefinitionIdOrderByDisplayOrder(definitionId);
        return buildFormResponse(def, response, defQuestions, items);
    }

    @Transactional
    public AppraisalFormResponse saveDraft(AppraisalFormSaveRequest req, String employeeId) {
        AppraisalResponse response = getEditableResponse(req.getDefinitionId(), employeeId,
                Set.of(AppraisalStatus.DRAFT, AppraisalStatus.REVOKED));
        response.setEmployeeOverallRemarks(req.getEmployeeOverallRemarks());
        saveItems(response, req.getItems(), false);
        responseRepo.save(response);
        return reloadForm(response);
    }

    @Transactional
    public AppraisalFormResponse submitForm(AppraisalFormSaveRequest req, String employeeId) {
        AppraisalResponse response = getEditableResponse(req.getDefinitionId(), employeeId,
                Set.of(AppraisalStatus.DRAFT, AppraisalStatus.REVOKED));
        validateSubmissionWindow(response.getDefinition());
        response.setEmployeeOverallRemarks(req.getEmployeeOverallRemarks());
        saveItems(response, req.getItems(), true);
        response.setStatus(AppraisalStatus.SUBMITTED);
        response.setSubmittedAt(LocalDateTime.now());
        responseRepo.save(response);
        return reloadForm(response);
    }

    private AppraisalResponse initResponse(AppraisalDefinition def, String employeeId,
                                           String l1ReviewerId, String l2ReviewerId) {
        AppraisalResponse r = new AppraisalResponse();
        r.setDefinition(def);
        r.setEmployeeId(employeeId);
        r.setStatus(AppraisalStatus.DRAFT);
        r.setL1ReviewerId(l1ReviewerId != null ? l1ReviewerId : def.getDefaultL1ReviewerId());
        r.setL2ReviewerId(l2ReviewerId != null ? l2ReviewerId : def.getDefaultL2ReviewerId());
        AppraisalResponse saved = responseRepo.save(r);
        List<AppraisalDefinitionQuestion> defQs =
                defQuestionRepo.findByDefinitionIdOrderByDisplayOrder(def.getId());
        List<AppraisalResponseItem> blankItems = defQs.stream().map(dq -> {
            AppraisalResponseItem item = new AppraisalResponseItem();
            item.setResponse(saved);
            item.setQuestion(dq.getQuestion());
            return item;
        }).collect(Collectors.toList());
        responseItemRepo.saveAll(blankItems);
        return saved;
    }

    private AppraisalResponse getEditableResponse(Long definitionId, String employeeId,
                                                  Set<AppraisalStatus> allowedStatuses) {
        AppraisalResponse response = responseRepo
                .findByDefinitionIdAndEmployeeId(definitionId, employeeId)
                .orElseThrow(() -> new RuntimeException("No appraisal form found for employee"));
        if (!allowedStatuses.contains(response.getStatus())) {
            throw new IllegalStateException("Form cannot be edited in status: " + response.getStatus());
        }
        return response;
    }

    private void validateSubmissionWindow(AppraisalDefinition def) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(def.getOpenDate()))
            throw new IllegalStateException("Submission window has not opened yet");
        if (today.isAfter(def.getCloseDate()))
            throw new IllegalStateException("Submission window has closed");
    }

    private void saveItems(AppraisalResponse response,
                           List<AppraisalFormSaveRequest.ItemRequest> itemRequests,
                           boolean validateRatings) {
        List<AppraisalResponseItem> existing = responseItemRepo.findByResponseId(response.getId());
        Map<Long, AppraisalResponseItem> byQuestion = existing.stream()
                .collect(Collectors.toMap(i -> i.getQuestion().getId(), i -> i));
        for (AppraisalFormSaveRequest.ItemRequest ir : itemRequests) {
            AppraisalResponseItem item = byQuestion.get(ir.getQuestionId());
            if (item == null) continue;
            if (validateRatings && ir.getSelfRating() == null) {
                throw new IllegalArgumentException("Self rating is required for question: " + ir.getQuestionId());
            }
            item.setEmployeeSelfRating(ir.getSelfRating());
            item.setEmployeeRemarks(ir.getRemarks());
        }
        responseItemRepo.saveAll(existing);
    }

    private AppraisalFormResponse reloadForm(AppraisalResponse response) {
        List<AppraisalResponseItem> items = responseItemRepo.findByResponseId(response.getId());
        List<AppraisalDefinitionQuestion> defQs =
                defQuestionRepo.findByDefinitionIdOrderByDisplayOrder(response.getDefinition().getId());
        return buildFormResponse(response.getDefinition(), response, defQs, items);
    }

    private AppraisalFormResponse buildFormResponse(AppraisalDefinition def,
                                                    AppraisalResponse response,
                                                    List<AppraisalDefinitionQuestion> defQs,
                                                    List<AppraisalResponseItem> items) {
        Map<Long, AppraisalResponseItem> itemByQuestion = items.stream()
                .collect(Collectors.toMap(i -> i.getQuestion().getId(), i -> i));
        Map<Long, AppraisalFormResponse.MetricSection> sectionMap = new LinkedHashMap<>();
        for (AppraisalDefinitionQuestion dq : defQs) {
            AppraisalQuestionMaster q = dq.getQuestion();
            AppraisalMetricMaster metric = q.getMetric();
            Long metricId = metric.getId();
            sectionMap.computeIfAbsent(metricId, k -> {
                AppraisalFormResponse.MetricSection section = new AppraisalFormResponse.MetricSection();
                section.setMetricId(metricId);
                section.setMetricType(metric.getMetricType());
                section.setMetricDescription(metric.getMetricDescription());
                section.setMinRating(metric.getMinRating());
                section.setMaxRating(metric.getMaxRating());
                section.setQuestions(new ArrayList<>());
                return section;
            });
            AppraisalResponseItem item = itemByQuestion.get(q.getId());
            AppraisalFormResponse.QuestionItem qi = new AppraisalFormResponse.QuestionItem();
            qi.setQuestionId(q.getId());
            qi.setQuestionText(q.getQuestionText());
            if (item != null) {
                qi.setItemId(item.getId());
                qi.setEmployeeSelfRating(item.getEmployeeSelfRating());
                qi.setEmployeeRemarks(item.getEmployeeRemarks());
                qi.setL1RevisedRating(item.getL1RevisedRating());
                qi.setL1RevisedRemarks(item.getL1RevisedRemarks());
                qi.setL2FinalRating(item.getL2FinalRating());
                qi.setL2FinalRemarks(item.getL2FinalRemarks());
            }
            sectionMap.get(metricId).getQuestions().add(qi);
        }
        AppraisalFormResponse resp = new AppraisalFormResponse();
        resp.setResponseId(response.getId());
        resp.setDefinitionId(def.getId());
        resp.setAppraisalYear(def.getAppraisalYear());
        resp.setTitle(def.getTitle());
        resp.setOpenDate(def.getOpenDate());
        resp.setCloseDate(def.getCloseDate());
        resp.setStatus(response.getStatus());
        resp.setEmployeeOverallRemarks(response.getEmployeeOverallRemarks());
        resp.setL1OverallRemarks(response.getL1OverallRemarks());
        resp.setL2OverallRemarks(response.getL2OverallRemarks());
        resp.setSections(new ArrayList<>(sectionMap.values()));
        return resp;
    }
}