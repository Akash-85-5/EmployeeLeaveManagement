package com.example.employeeLeaveApplication.repository;

import com.example.employeeLeaveApplication.entity.Meeting;
import com.example.employeeLeaveApplication.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // ✅ Existing: Check for team-wide overlaps
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.teamId = :teamId
          AND m.status = 'SCHEDULED'
          AND m.startTime < :endTime
          AND m.endTime > :startTime
    """)
    List<Meeting> findOverlappingMeetings(
            @Param("teamId") Long teamId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ✅ NEW: Check if a specific person (HR) is already booked in another meeting
    @Query("""
        SELECT COUNT(m) > 0 FROM Meeting m 
        JOIN m.attendees a 
        WHERE a.id = :employeeId 
          AND m.status = 'SCHEDULED' 
          AND m.startTime < :endTime 
          AND m.endTime > :startTime
    """)
    boolean existsOverlappingMeetingForAttendee(
            @Param("employeeId") Long employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
//    List<Meeting> findByManagerId(Long managerId);

    List<Meeting> findByCreatedBy(Long employeeId);

    List<Meeting> findByStatus(MeetingStatus status);

    @Query("""
SELECT m FROM Meeting m
JOIN Employee e ON m.createdBy = e.id
WHERE e.managerId = :managerId
AND m.status = com.example.employeeLeaveApplication.enums.MeetingStatus.PENDING
""")
    List<Meeting> findPendingForManager(Long managerId);
}