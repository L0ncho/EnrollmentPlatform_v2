package com.duoc.enrollmentplatform.enrollment.application;

import com.duoc.enrollmentplatform.courses.domain.entities.Course;
import com.duoc.enrollmentplatform.courses.domain.repositories.CourseRepository;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Enrollment;
import com.duoc.enrollmentplatform.enrollment.domain.entities.EnrollmentLine;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Student;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.EnrollmentRepository;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.StudentRepository;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentSummaryStorage;
import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher;
import com.duoc.enrollmentplatform.enrollment.application.summary.EnrollmentSummaryGenerator;
import com.duoc.enrollmentplatform.shared.domain.DomainError;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Id;

import java.util.List;

public class CreateEnrollmentUseCase {
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSummaryGenerator summaryGenerator;
    private final EnrollmentSummaryStorage summaryStorage;
    private final EnrollmentMessagePublisher messagePublisher; // NUEVO
    private final EnrollmentDtoMapper mapper;

    public CreateEnrollmentUseCase(CourseRepository courseRepository,
                                   StudentRepository studentRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   EnrollmentSummaryGenerator summaryGenerator,
                                   EnrollmentSummaryStorage summaryStorage,
                                   EnrollmentMessagePublisher messagePublisher) { // NUEVO
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.summaryGenerator = summaryGenerator;
        this.summaryStorage = summaryStorage;
        this.messagePublisher = messagePublisher; // NUEVO
        this.mapper = new EnrollmentDtoMapper();
    }

    public EnrollmentSummaryDTO execute(String studentId, List<String> courseIds) {
        Student student = studentRepository.findById(Id.create(studentId))
                .orElseThrow(() -> DomainError.notFound("Student " + studentId + " not found"));

        List<EnrollmentLine> lines = courseIds.stream()
                .map(courseId -> {
                    Course course = courseRepository.findById(Id.create(courseId))
                            .orElseThrow(() -> DomainError.notFound("Course " + courseId + " not found"));
                    return EnrollmentLine.create(Id.generate(), course.getId(), course.getName(), course.getPrice());
                })
                .toList();

        Enrollment enrollment = Enrollment.create(Id.generate(), Id.create(studentId), lines);
        enrollmentRepository.save(enrollment);

        String enrollmentId = enrollment.getId().getValue();
        summaryStorage.upload(enrollmentId, summaryGenerator.toJsonBytes(enrollment, student));

        // NUEVO: Enviamos el mensaje a la cola RabbitMQ
        messagePublisher.publish(enrollment);

        return mapper.toSummaryDto(enrollment);
    }
}