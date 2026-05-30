package com.duoc.enrollmentplatform.enrollment.application;

public class EnrollmentLineDTO {
    public String courseId;
    public String courseName;
    public double unitPrice;

    public EnrollmentLineDTO() {
    }

    public EnrollmentLineDTO(String courseId, String courseName, double unitPrice) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.unitPrice = unitPrice;
    }
}
