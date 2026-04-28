package com.emp_management.feature.appraisal.service;

import com.emp_management.feature.appraisal.dto.AppraisalDefinitionRequest;
import com.emp_management.feature.appraisal.entity.AppraisalDefinition;
import com.emp_management.feature.appraisal.entity.AppraisalDefinitionQuestion;
import com.emp_management.feature.appraisal.entity.AppraisalQuestionMaster;
import com.emp_management.feature.appraisal.repository.AppraisalDefinitionQuestionRepository;
import com.emp_management.feature.appraisal.repository.AppraisalDefinitionRepository;
import com.emp_management.feature.appraisal.repository.AppraisalQuestionMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AppraisalDefinitionService {

    private final AppraisalDefinitionRepository definitionRepo;
    private final AppraisalDefinitionQuestionRepository defQuestionRepo;
    private final AppraisalQuestionMasterRepository questionMasterRepo;

    public AppraisalDefinitionService(AppraisalDefinitionRepository definitionRepo,
                                      AppraisalDefinitionQuestionRepository defQuestionRepo,
                                      AppraisalQuestionMasterRepository questionMasterRepo) {
        this.definitionRepo = definitionRepo;
        this.defQuestionRepo = defQuestionRepo;
        this.questionMasterRepo = questionMasterRepo;
    }

    public AppraisalDefinition createDefinition(AppraisalDefinitionRequest req, String createdBy) {
        if (!req.getOpenDate().isBefore(req.getCloseDate())) {
            throw new IllegalArgumentException("openDate must be before closeDate");
        }
        AppraisalDefinition def = new AppraisalDefinition();
        def.setAppraisalYear(req.getAppraisalYear());
        def.setTitle(req.getTitle());
        def.setOpenDate(req.getOpenDate());
        def.setCloseDate(req.getCloseDate());
        def.setDefaultL1ReviewerId(req.getDefaultL1ReviewerId());
        def.setDefaultL2ReviewerId(req.getDefaultL2ReviewerId());
        def.setAutoSubmitEnabled(req.getAutoSubmitEnabled());
        def.setIsPublished(false);
        def.setCreatedBy(createdBy);
        AppraisalDefinition saved = definitionRepo.save(def);
        attachQuestions(saved, req.getQuestionIds());
        return saved;
    }

    public AppraisalDefinition updateDefinition(Long id, AppraisalDefinitionRequest req, String updatedBy) {
        AppraisalDefinition def = definitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + id));
        if (def.getIsPublished()) {
            throw new IllegalStateException("Cannot edit a published appraisal definition");
        }
        def.setAppraisalYear(req.getAppraisalYear());
        def.setTitle(req.getTitle());
        def.setOpenDate(req.getOpenDate());
        def.setCloseDate(req.getCloseDate());
        def.setDefaultL1ReviewerId(req.getDefaultL1ReviewerId());
        def.setDefaultL2ReviewerId(req.getDefaultL2ReviewerId());
        def.setAutoSubmitEnabled(req.getAutoSubmitEnabled());
        def.setUpdatedBy(updatedBy);
        defQuestionRepo.deleteByDefinitionId(id);
        attachQuestions(def, req.getQuestionIds());
        return definitionRepo.save(def);
    }

    public AppraisalDefinition publishDefinition(Long id, String updatedBy) {
        AppraisalDefinition def = definitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + id));
        if (def.getIsPublished()) {
            throw new IllegalStateException("Already published");
        }
        def.setIsPublished(true);
        def.setUpdatedBy(updatedBy);
        return definitionRepo.save(def);
    }

    @Transactional(readOnly = true)
    public AppraisalDefinition getById(Long id) {
        return definitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AppraisalDefinition> getAllPublished() {
        return definitionRepo.findByIsPublishedTrue();
    }

    @Transactional(readOnly = true)
    public List<AppraisalDefinition> getAll() {
        return definitionRepo.findAll();
    }

    private void attachQuestions(AppraisalDefinition def, List<Long> questionIds) {
        List<AppraisalDefinitionQuestion> links = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            Long qId = questionIds.get(i);
            AppraisalQuestionMaster q = questionMasterRepo.findById(qId)
                    .orElseThrow(() -> new RuntimeException("Question not found: " + qId));
            AppraisalDefinitionQuestion link = new AppraisalDefinitionQuestion();
            link.setDefinition(def);
            link.setQuestion(q);
            link.setDisplayOrder(i + 1);
            links.add(link);
        }
        defQuestionRepo.saveAll(links);
    }
}