package com.duoc.enrollmentplatform.enrollment.infrastructure.adapters;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEnrollmentSummaryRecordRepository extends JpaRepository<EnrollmentSummaryRecord, String> {
}