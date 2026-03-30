package com.emp_management.feature.employee.entity;

import com.emp_management.shared.enums.SeparationStatus;
import com.emp_management.shared.enums.SeparationType;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_separation")
public class EmployeeSeparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private SeparationStatus separationStatus;

    @Enumerated(EnumType.STRING)
    private SeparationType separationType;

    private LocalDate noticeStartDate;
    private LocalDate noticeEndDate;
    private LocalDate relievedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public SeparationStatus getSeparationStatus() {
        return separationStatus;
    }

    public void setSeparationStatus(SeparationStatus separationStatus) {
        this.separationStatus = separationStatus;
    }

    public SeparationType getSeparationType() {
        return separationType;
    }

    public void setSeparationType(SeparationType separationType) {
        this.separationType = separationType;
    }

    public LocalDate getNoticeStartDate() {
        return noticeStartDate;
    }

    public void setNoticeStartDate(LocalDate noticeStartDate) {
        this.noticeStartDate = noticeStartDate;
    }

    public LocalDate getNoticeEndDate() {
        return noticeEndDate;
    }

    public void setNoticeEndDate(LocalDate noticeEndDate) {
        this.noticeEndDate = noticeEndDate;
    }

    public LocalDate getRelievedDate() {
        return relievedDate;
    }

    public void setRelievedDate(LocalDate relievedDate) {
        this.relievedDate = relievedDate;
    }
}
