package com.emp_management.feature.workfromhome.repository;

import com.emp_management.feature.workfromhome.entity.WfhReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WfhReminderRepository extends JpaRepository<WfhReminder, Long> {

    Optional<WfhReminder> findByWfhRequestId(Long wfhRequestId);
}
