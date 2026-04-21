package com.emp_management.feature.auth.repository;

import com.emp_management.feature.auth.entity.PasswordAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PasswordAuditRepository extends JpaRepository<PasswordAudit, Long> {

    /**
     * Returns the last N audit rows for a user, newest first.
     * We only ever need the last 3, so always pass 3 as the limit.
     */
    List<PasswordAudit> findTop3ByUserIdOrderByCreatedAtDesc(String userId);

    /** Count total entries for a user — used to decide whether to prune. */
    long countByUserId(String userId);

    /**
     * Delete the oldest entry that exceeds the 3-entry cap.
     * Called after every insert when count > 3.
     */
    @Modifying
    @Transactional
    @Query(value = """
    DELETE pa FROM password_audit pa
    JOIN (
        SELECT id FROM password_audit
        WHERE user_id = :userId
        ORDER BY created_at ASC
        LIMIT 1
    ) oldest ON pa.id = oldest.id
    """, nativeQuery = true)
    void deleteOldestByUserId(String userId);
}