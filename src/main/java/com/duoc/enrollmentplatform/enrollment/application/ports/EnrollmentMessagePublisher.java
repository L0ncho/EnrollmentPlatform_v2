package com.duoc.enrollmentplatform.enrollment.application.ports;

import com.duoc.enrollmentplatform.enrollment.domain.entities.Enrollment;

public interface EnrollmentMessagePublisher {
    void publish(Enrollment enrollment);
}