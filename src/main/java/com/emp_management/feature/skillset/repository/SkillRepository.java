package com.emp_management.feature.skillset.repository;

import com.emp_management.feature.skillset.entity.Skill;
import com.emp_management.shared.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // ── My skills (by JWT employee id) ───────────────────────────────────
    List<Skill> findByEmployee_EmpIdOrderByCreatedAtDesc(String empId);

    // ── Single skill with ownership check ─────────────────────────────────
    Optional<Skill> findByIdAndEmployee_EmpId(Long id, String empId);

    // ── Skills for a specific employee (HR / manager view) ────────────────
    List<Skill> findByEmployee_EmpIdOrderBySkillNameAsc(String empId);

    // ── Team skills: all employees who report to a manager ────────────────
    @Query("""
        SELECT s FROM Skill s
        JOIN FETCH s.employee e
        WHERE e.reportingId = :managerId
          AND e.active = true
        ORDER BY e.name ASC, s.skillName ASC
    """)
    List<Skill> findAllByManagerId(@Param("managerId") String managerId);

    // ── Badge / progression stats: counts per category ────────────────────
    @Query("""
        SELECT COUNT(s) FROM Skill s
        WHERE s.employee.empId = :empId
          AND s.category = :category
    """)
    int countByEmpIdAndCategory(@Param("empId") String empId,
                                @Param("category") SkillCategory category);

    // ── Average rating per category ───────────────────────────────────────
    @Query("""
        SELECT AVG(s.rating) FROM Skill s
        WHERE s.employee.empId = :empId
          AND s.category = :category
          AND s.rating IS NOT NULL
    """)
    Double avgRatingByEmpIdAndCategory(@Param("empId") String empId,
                                       @Param("category") SkillCategory category);

    // ── Check skill exists for an employee (prevent duplicate names) ───────
    @Query("""
        SELECT COUNT(s) > 0 FROM Skill s
        WHERE s.employee.empId = :empId
          AND LOWER(s.skillName) = LOWER(:skillName)
          AND (:excludeId IS NULL OR s.id <> :excludeId)
    """)
    boolean existsByEmpIdAndSkillName(@Param("empId") String empId,
                                      @Param("skillName") String skillName,
                                      @Param("excludeId") Long excludeId);
}