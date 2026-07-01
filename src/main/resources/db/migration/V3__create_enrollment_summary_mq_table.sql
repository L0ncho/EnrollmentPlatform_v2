CREATE TABLE enrollment_summary_mq (
    id VARCHAR2(36) PRIMARY KEY,
    enrollment_id VARCHAR2(36) NOT NULL,
    student_id VARCHAR2(36) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);