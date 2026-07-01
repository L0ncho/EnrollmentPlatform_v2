package com.duoc.enrollmentplatform.factory;

import com.duoc.enrollmentplatform.courses.application.CreateCourseUseCase;
import com.duoc.enrollmentplatform.courses.application.ListCoursesUseCase;
import com.duoc.enrollmentplatform.courses.domain.repositories.CourseRepository;
import com.duoc.enrollmentplatform.courses.infrastructure.adapters.CourseStore;
import com.duoc.enrollmentplatform.courses.infrastructure.adapters.JpaCourseRepository;
import com.duoc.enrollmentplatform.courses.infrastructure.http.CourseController;
import com.duoc.enrollmentplatform.enrollment.application.CreateEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.application.DeleteEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.application.GetEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.application.ListEnrollmentsUseCase;
import com.duoc.enrollmentplatform.enrollment.application.UpdateEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher; // <-- IMPORT AGREGADO
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentSummaryPdfRenderer;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentSummaryStorage;
import com.duoc.enrollmentplatform.enrollment.application.summary.DeleteEnrollmentSummaryUseCase;
import com.duoc.enrollmentplatform.enrollment.application.summary.DownloadEnrollmentSummaryUseCase;
import com.duoc.enrollmentplatform.enrollment.application.summary.EnrollmentSummaryGenerator;
import com.duoc.enrollmentplatform.enrollment.application.summary.GenerateEnrollmentSummaryFileUseCase;
import com.duoc.enrollmentplatform.enrollment.application.summary.ListEnrollmentSummariesUseCase;
import com.duoc.enrollmentplatform.enrollment.application.summary.ReplaceEnrollmentSummaryUseCase;
import com.duoc.enrollmentplatform.enrollment.application.summary.UploadEnrollmentSummaryUseCase;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.EnrollmentRepository;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.StudentRepository;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.EnrollmentStore;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.JpaEnrollmentRepository;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.JpaStudentRepository;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.OpenPdfEnrollmentSummaryRenderer;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.StudentStore;
import com.duoc.enrollmentplatform.enrollment.infrastructure.http.EnrollmentController;
import com.duoc.enrollmentplatform.enrollment.infrastructure.http.EnrollmentSummaryController;

public class EnrollmentPlatformFactory {

    public static CourseRepository getCourseRepository(CourseStore store) {
        return new JpaCourseRepository(store);
    }

    public static StudentRepository getStudentRepository(StudentStore store) {
        return new JpaStudentRepository(store);
    }

    public static EnrollmentRepository getEnrollmentRepository(EnrollmentStore store) {
        return new JpaEnrollmentRepository(store);
    }

    public static EnrollmentSummaryGenerator enrollmentSummaryGenerator() {
        return new EnrollmentSummaryGenerator();
    }

    public static EnrollmentSummaryPdfRenderer enrollmentSummaryPdfRenderer() {
        return new OpenPdfEnrollmentSummaryRenderer();
    }

    public static CourseController createCourseController(CourseRepository courseRepository) {
        return new CourseController(
                new ListCoursesUseCase(courseRepository),
                new CreateCourseUseCase(courseRepository)
        );
    }

    // <-- MÉTODO MODIFICADO PARA RECIBIR EL messagePublisher Y PASARLO AL CreateEnrollmentUseCase
    public static EnrollmentController createEnrollmentController(
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSummaryGenerator summaryGenerator,
            EnrollmentSummaryStorage summaryStorage,
            EnrollmentMessagePublisher messagePublisher) { // <-- PARÁMETRO AGREGADO
        return new EnrollmentController(
                new CreateEnrollmentUseCase(courseRepository, studentRepository, enrollmentRepository, summaryGenerator, summaryStorage, messagePublisher), // <-- INYECTADO AQUÍ
                new ListEnrollmentsUseCase(enrollmentRepository),
                new GetEnrollmentUseCase(enrollmentRepository),
                new UpdateEnrollmentUseCase(courseRepository, enrollmentRepository, studentRepository, summaryGenerator, summaryStorage),
                new DeleteEnrollmentUseCase(enrollmentRepository, summaryStorage)
        );
    }

    public static EnrollmentSummaryController createEnrollmentSummaryController(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            EnrollmentSummaryGenerator summaryGenerator,
            EnrollmentSummaryStorage summaryStorage,
            EnrollmentSummaryPdfRenderer pdfRenderer) {
        return new EnrollmentSummaryController(
                new GenerateEnrollmentSummaryFileUseCase(enrollmentRepository, studentRepository, summaryGenerator),
                new UploadEnrollmentSummaryUseCase(enrollmentRepository, studentRepository, summaryGenerator, summaryStorage),
                new DownloadEnrollmentSummaryUseCase(summaryStorage, pdfRenderer),
                new ReplaceEnrollmentSummaryUseCase(summaryStorage),
                new DeleteEnrollmentSummaryUseCase(summaryStorage),
                new ListEnrollmentSummariesUseCase(summaryStorage)
        );
    }
}