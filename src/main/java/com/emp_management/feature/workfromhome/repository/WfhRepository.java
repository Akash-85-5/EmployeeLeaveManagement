package com.emp_management.feature.workfromhome.repository;

import com.emp_management.feature.workfromhome.entity.WfhRequest;
import com.emp_management.shared.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WfhRepository extends JpaRepository<WfhRequest, Long> {

    // Manager view
    List<WfhRequest> findByCurrentApprover(String approverId);

    // Employee view
    List<WfhRequest> findByEmployee_EmpId(String empId);
    List<WfhRequest> findByEmployee_EmpIdAndStatusIn(
            String empId,
            List<RequestStatus> statuses
    );
}