package com.duoc.enrollmentplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;

@SpringBootApplication(exclude = OAuth2ResourceServerAutoConfiguration.class)
public class EnrollmentPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnrollmentPlatformApplication.class, args);
	}
}
