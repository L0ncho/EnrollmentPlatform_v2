package com.duoc.enrollmentplatform.enrollment.application.summary;

import com.duoc.enrollmentplatform.enrollment.application.EnrollmentLineDTO;

import java.util.List;

public class EnrollmentSummaryDocumentDTO {
    public String enrollmentId;
    public String studentId;
    public String studentFullName;
    public String studentEmail;
    public String enrolledAt;
    public List<EnrollmentLineDTO> lines;
    public double totalAmount;

    public EnrollmentSummaryDocumentDTO() {
    }

    public EnrollmentSummaryDocumentDTO(String enrollmentId, String studentId, String studentFullName,
                                        String studentEmail, String enrolledAt, List<EnrollmentLineDTO> lines,
                                        double totalAmount) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentFullName = studentFullName;
        this.studentEmail = studentEmail;
        this.enrolledAt = enrolledAt;
        this.lines = lines;
        this.totalAmount = totalAmount;
    }
}
