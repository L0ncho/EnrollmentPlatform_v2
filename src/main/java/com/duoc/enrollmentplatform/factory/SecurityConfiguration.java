package com.duoc.enrollmentplatform.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "enrollment.security.jwt.enabled", havingValue = "true")
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(
            @Value("${AZURE_B2C_JWK_SET_URI:}") String jwkSetUri,
            @Value("${AZURE_B2C_AUDIENCE:}") String audience) {
        validateJwtProperties(jwkSetUri, audience);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(audience));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                audienceValidator));
        return jwtDecoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${enrollment.security.jwt.enabled:false}") boolean jwtEnabled,
            @Value("${AZURE_B2C_JWK_SET_URI:}") String jwkSetUri,
            @Value("${AZURE_B2C_AUDIENCE:}") String audience) throws Exception {
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
