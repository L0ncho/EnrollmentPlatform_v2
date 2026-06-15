package com.duoc.enrollmentplatform.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${enrollment.security.jwt.enabled:false}") boolean jwtEnabled,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.audiences:}") String audience) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        if (jwtEnabled) {
            validateJwtProperties(jwkSetUri, audience);
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    private void validateJwtProperties(String jwkSetUri, String audience) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new IllegalStateException(
                    "ENROLLMENT_SECURITY_JWT_ENABLED=true requires AZURE_B2C_JWK_SET_URI to be set");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException(
                    "ENROLLMENT_SECURITY_JWT_ENABLED=true requires AZURE_B2C_AUDIENCE to be set");
        }
    }
}
