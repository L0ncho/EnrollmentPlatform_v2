package com.duoc.enrollmentplatform.enrollment.infrastructure.adapters;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment_summary_mq")
public class EnrollmentSummaryRecord {
    
    @Id
    private String id;
    private String enrollmentId;
    private String studentId;
    private String status;
    private LocalDateTime createdAt;

    // Constructor vacío exigido por JPA
    public EnrollmentSummaryRecord() {}

    public EnrollmentSummaryRecord(String id, String enrollmentId, String studentId, String status) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getEnrollmentId() { return enrollmentId; }
    public String getStudentId() { return studentId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}