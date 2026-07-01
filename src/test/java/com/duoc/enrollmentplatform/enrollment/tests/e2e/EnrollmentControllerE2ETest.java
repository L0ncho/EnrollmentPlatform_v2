package com.duoc.enrollmentplatform.enrollment.tests.e2e;

import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Import(EnrollmentControllerE2ETest.MockConfig.class)
class EnrollmentControllerE2ETest {

    // Le pasamos el mock directamente al contexto sin usar @MockBean
    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public EnrollmentMessagePublisher enrollmentMessagePublisher() {
            return mock(EnrollmentMessagePublisher.class);
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

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