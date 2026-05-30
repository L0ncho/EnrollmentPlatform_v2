package com.duoc.enrollmentplatform.enrollment.tests.unit;

import com.duoc.enrollmentplatform.enrollment.application.summary.EnrollmentSummaryGenerator;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Enrollment;
import com.duoc.enrollmentplatform.enrollment.domain.entities.EnrollmentLine;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Student;
import com.duoc.enrollmentplatform.enrollment.infrastructure.adapters.OpenPdfEnrollmentSummaryRenderer;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Email;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Id;
import com.duoc.enrollmentplatform.shared.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenPdfEnrollmentSummaryRendererTest {

    @Test
    void rendersPdfFromGeneratedSummaryJson() {
        Enrollment enrollment = Enrollment.create(Id.create("e-1"), Id.create("s-1"), List.of(
                EnrollmentLine.create(Id.generate(), Id.create("c-1"), "Intro Java", Money.create(150000))));
        Student student = Student.create(Id.create("s-1"), "Juan Soto", Email.create("juan@duoc.cl"));

        byte[] json = new EnrollmentSummaryGenerator().toJsonBytes(enrollment, student);
        byte[] pdf = new OpenPdfEnrollmentSummaryRenderer().render(json);

        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
    }
}
