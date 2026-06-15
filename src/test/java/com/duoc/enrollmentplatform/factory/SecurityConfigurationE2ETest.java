package com.duoc.enrollmentplatform.factory;

import com.duoc.enrollmentplatform.EnrollmentPlatformApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
class SecurityConfigurationDisabledE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void allowsAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk());
    }
}

@SpringBootTest(properties = {
        "enrollment.security.jwt.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com/keys",
        "spring.security.oauth2.resourceserver.jwt.audiences=test-audience"
})
@ActiveProfiles("local")
@Import(SecurityConfigurationEnabledE2ETest.TestJwtDecoderConfiguration.class)
class SecurityConfigurationEnabledE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsRequestWithJwt() throws Exception {
        mockMvc.perform(get("/courses").with(jwt()))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestJwtDecoderConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test-user")
                    .audience(List.of("test-audience"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}

class SecurityConfigurationFailFastTest {

    @Test
    void failsToStartApplicationWhenJwtEnabledWithoutAzureConfig() {
        assertThatThrownBy(() -> new SpringApplication(EnrollmentPlatformApplication.class)
                        .run(
                                "--spring.profiles.active=local",
                                "--enrollment.security.jwt.enabled=true",
                                "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=",
                                "--spring.security.oauth2.resourceserver.jwt.audiences="))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(
                        "ENROLLMENT_SECURITY_JWT_ENABLED=true requires AZURE_B2C_JWK_SET_URI to be set");
    }
}
