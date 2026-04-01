package com.emp_management.feature.leave.annual.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_attachment")
public class LeaveAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leave_application_id", nullable = false)
    private Long leaveApplicationId;

    @Column(name = "file_name", nullable = false)
    private String fileName;           // original file name shown to user

    @Column(name = "file_url", nullable = false)
    private String fileUrl;            // stored file path/name on disk

    @Column(name = "file_type")
    private String fileType;           // e.g. image/jpeg, application/pdf

    @Column(name = "file_size")
    private Long fileSize;             // bytes

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "leave_application_id", insertable = false, updatable = false)
    private LeaveApplication leaveApplication;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeaveApplicationId() { return leaveApplicationId; }
    public void setLeaveApplicationId(Long leaveApplicationId) { this.leaveApplicationId = leaveApplicationId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LeaveApplication getLeaveApplication() { return leaveApplication; }
    public void setLeaveApplication(LeaveApplication leaveApplication) { this.leaveApplication = leaveApplication; }
}