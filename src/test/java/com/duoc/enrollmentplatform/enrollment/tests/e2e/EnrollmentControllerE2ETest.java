package com.duoc.enrollmentplatform.enrollment.tests.e2e;

import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
class EnrollmentControllerE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private EnrollmentMessagePublisher enrollmentMessagePublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void enrollsStudentInSeededCourses() throws Exception {
        String body = """
                {
                  "studentId": "s-001",
                  "courseIds": ["c-001", "c-002"]
                }
                """;
        mockMvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    void returns404WhenStudentDoesNotExist() throws Exception {
        String body = """
                {
                  "studentId": "unknown-student",
                  "courseIds": ["c-001"]
                }
                """;
        mockMvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns422WhenCourseListIsEmpty() throws Exception {
        String body = """
                {
                  "studentId": "s-001",
                  "courseIds": []
                }
                """;
        mockMvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void returns404WhenCourseDoesNotExist() throws Exception {
        String body = """
                {
                  "studentId": "s-001",
                  "courseIds": ["non-existent-course"]
                }
                """;
        mockMvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}