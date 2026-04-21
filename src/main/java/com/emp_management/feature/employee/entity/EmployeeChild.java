package com.emp_management.feature.employee.entity;

import com.emp_management.shared.enums.Gender;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_child")
public class EmployeeChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many children → one personal details record.
     * Cascade ALL so children are saved/deleted with the parent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_details_id", nullable = false)
    private EmployeePersonalDetails personalDetails;

    @Column(name = "child_name", length = 100, nullable = false)
    private String childName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    /** Age in years. */
    @Column(name = "child_date_of_birth", nullable = false)
    private LocalDate childDateOfBirth;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmployeePersonalDetails getPersonalDetails() { return personalDetails; }
    public void setPersonalDetails(EmployeePersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public LocalDate getChildDateOfBirth() {
        return childDateOfBirth;
    }

    public void setChildDateOfBirth(LocalDate childDateOfBirth) {
        this.childDateOfBirth = childDateOfBirth;
    }
}