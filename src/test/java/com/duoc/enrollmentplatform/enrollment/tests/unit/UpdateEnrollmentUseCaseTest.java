package com.duoc.enrollmentplatform.enrollment.tests.unit;

import com.duoc.enrollmentplatform.courses.domain.entities.Course;
import com.duoc.enrollmentplatform.courses.domain.repositories.InMemoryCourseRepository;
import com.duoc.enrollmentplatform.enrollment.application.CreateEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.application.EnrollmentSummaryDTO;
import com.duoc.enrollmentplatform.enrollment.application.summary.EnrollmentSummaryGenerator;
import com.duoc.enrollmentplatform.enrollment.application.UpdateEnrollmentUseCase;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Student;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.InMemoryEnrollmentRepository;
import com.duoc.enrollmentplatform.enrollment.domain.repositories.InMemoryStudentRepository;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.InMemoryEnrollmentSummaryStorage;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Email;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Id;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateEnrollmentUseCaseTest {

    @Test
    void replacesSummaryInStorageWhenEnrollmentIsUpdated() throws Exception {
        InMemoryEnrollmentSummaryStorage storage = new InMemoryEnrollmentSummaryStorage();
        EnrollmentSummaryGenerator generator = new EnrollmentSummaryGenerator();
        InMemoryCourseRepository courses = new InMemoryCourseRepository(List.of(
                Course.create(Id.create("c-1"), "Intro", "A", 10, Money.create(150000)),
                Course.create(Id.create("c-2"), "DB", "B", 10, Money.create(120000))));
        InMemoryStudentRepository students = new InMemoryStudentRepository(List.of(
                Student.create(Id.create("s-1"), "Juan", Email.create("juan@duoc.cl"))));
        InMemoryEnrollmentRepository enrollments = new InMemoryEnrollmentRepository();

        CreateEnrollmentUseCase create = new CreateEnrollmentUseCase(courses, students, enrollments, generator, storage, null);
        EnrollmentSummaryDTO created = create.execute("s-1", List.of("c-1"));

        UpdateEnrollmentUseCase update = new UpdateEnrollmentUseCase(courses, enrollments, students, generator, storage);
        EnrollmentSummaryDTO updated = update.execute(created.enrollmentId, List.of("c-1", "c-2"));

        assertEquals(270000, updated.totalAmount);
        byte[] stored = storage.download(created.enrollmentId).orElseThrow().content;
        JsonNode root = new ObjectMapper().readTree(stored);
        assertEquals(270000, root.get("totalAmount").asDouble());
    }
}
