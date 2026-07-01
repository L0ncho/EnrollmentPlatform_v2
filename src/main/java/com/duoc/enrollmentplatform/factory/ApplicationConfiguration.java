package com.duoc.enrollmentplatform.factory;

import com.duoc.enrollmentplatform.courses.domain.repositories.CourseRepository;
import com.duoc.enrollmentplatform.courses.infrastructure.adapters.CourseStore;
import com.duoc.enrollmentplatform.courses.infrastructure.http.CourseController;
import com.duoc.enrollmentplatform.enrollment.application.summary.EnrollmentSummaryGenerator;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentSummaryPdfRenderer;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentSummaryStorage;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.EnrollmentRepository;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.StudentRepository;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.EnrollmentStore;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.StudentStore;
import com.duoc.enrollmentplatform.enrollment.infrastructure.http.EnrollmentController;
import com.duoc.enrollmentplatform.enrollment.infrastructure.http.EnrollmentSummaryController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public CourseRepository courseRepository(CourseStore store) {
        return EnrollmentPlatformFactory.getCourseRepository(store);
    }

    @Bean
    public StudentRepository studentRepository(StudentStore store) {
        return EnrollmentPlatformFactory.getStudentRepository(store);
    }

    @Bean
    public EnrollmentRepository enrollmentRepository(EnrollmentStore store) {
        return EnrollmentPlatformFactory.getEnrollmentRepository(store);
    }

    @Bean
    public EnrollmentSummaryGenerator enrollmentSummaryGenerator() {
        return EnrollmentPlatformFactory.enrollmentSummaryGenerator();
    }

    @Bean
    public EnrollmentSummaryPdfRenderer enrollmentSummaryPdfRenderer() {
        return EnrollmentPlatformFactory.enrollmentSummaryPdfRenderer();
    }

    @Bean
    public CourseController courseController(CourseRepository courseRepository) {
        return EnrollmentPlatformFactory.createCourseController(courseRepository);
    }

    @Bean
    public EnrollmentController enrollmentController(
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSummaryGenerator enrollmentSummaryGenerator,
            EnrollmentSummaryStorage enrollmentSummaryStorage,
            com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher enrollmentMessagePublisher) { // <-- AGREGADO
        
        return EnrollmentPlatformFactory.createEnrollmentController(
                courseRepository, studentRepository, enrollmentRepository,
                enrollmentSummaryGenerator, enrollmentSummaryStorage, 
                enrollmentMessagePublisher); // <-- PASADO A LA FACTORY
    }

    @Bean
    public EnrollmentSummaryController enrollmentSummaryController(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            EnrollmentSummaryGenerator enrollmentSummaryGenerator,
            EnrollmentSummaryStorage enrollmentSummaryStorage,
            EnrollmentSummaryPdfRenderer enrollmentSummaryPdfRenderer) {
        return EnrollmentPlatformFactory.createEnrollmentSummaryController(
                enrollmentRepository, studentRepository, enrollmentSummaryGenerator,
                enrollmentSummaryStorage, enrollmentSummaryPdfRenderer);
    }
}
