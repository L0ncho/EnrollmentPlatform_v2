package com.duoc.enrollmentplatform.enrollment.tests.e2e;

import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Import(EnrollmentSummaryControllerE2ETest.MockConfig.class)
class EnrollmentSummaryControllerE2ETest {

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

    private MockMvc mockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
        return mockMvc;
    }

    @Test
    void managesEnrollmentSummaryLifecycle() throws Exception {
        String createBody = """
                {
                  "studentId": "s-001",
                  "courseIds": ["c-001", "c-002"]
                }
                """;
        MvcResult createResult = mockMvc().perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andReturn();

        JsonNode created = new ObjectMapper().readTree(createResult.getResponse().getContentAsString());
        String enrollmentId = created.get("enrollmentId").asText();

        mockMvc().perform(get("/enrollments/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].enrollmentId", hasItem(enrollmentId)));

        mockMvc().perform(get("/enrollments/{id}/summary", enrollmentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("summary-" + enrollmentId)));

        mockMvc().perform(get("/enrollments/{id}/summary", enrollmentId).param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        mockMvc().perform(put("/enrollments/{id}", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "courseIds": ["c-001"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(150000.0));

        mockMvc().perform(get("/enrollments/{id}/summary", enrollmentId))
                .andExpect(status().isOk());

        mockMvc().perform(delete("/enrollments/{id}/summary", enrollmentId))
                .andExpect(status().isNoContent());

        mockMvc().perform(get("/enrollments/{id}/summary", enrollmentId))
                .andExpect(status().isNotFound());

        mockMvc().perform(delete("/enrollments/{id}", enrollmentId))
                .andExpect(status().isNoContent());

        mockMvc().perform(get("/enrollments/{id}", enrollmentId))
                .andExpect(status().isNotFound());
    }
}