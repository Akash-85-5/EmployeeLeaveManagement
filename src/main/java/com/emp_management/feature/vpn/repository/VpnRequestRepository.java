package com.emp_management.feature.vpn.repository;

import com.emp_management.feature.vpn.entity.VpnRequest;
import com.emp_management.shared.enums.VpnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpnRequestRepository extends JpaRepository<VpnRequest, Long> {

    List<VpnRequest> findByApplicantIdOrderByCreatedAtDesc(String applicantId);

    List<VpnRequest> findByManagerApproverIdAndStatusOrderByCreatedAtDesc(
            String managerApproverId,
            VpnRequestStatus status
    );

    List<VpnRequest> findByManagerApproverIdOrderByCreatedAtDesc(String managerApproverId);

    List<VpnRequest> findByStatusOrderByCreatedAtDesc(VpnRequestStatus status);

    // FIX: needed for "admin/all" to show all non-pending-manager requests
    List<VpnRequest> findAllByOrderByCreatedAtDesc();
}